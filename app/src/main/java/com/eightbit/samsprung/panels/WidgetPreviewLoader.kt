package com.eightbit.samsprung.panels

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.content.res.Resources.NotFoundException
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprung
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors
import android.graphics.Bitmap




internal abstract class SoftReferenceThreadLocal<T> {
    private val mThreadLocal: ThreadLocal<SoftReference<T?>>
    abstract fun initialValue(): T
    fun set(t: T) {
        mThreadLocal.set(SoftReference(t))
    }

    fun get(): T? {
        val reference = mThreadLocal.get()
        var obj: T?
        if (reference == null) {
            obj = initialValue()
            mThreadLocal.set(SoftReference(obj))
        } else {
            obj = reference.get()
            if (obj == null) {
                obj = initialValue()
                mThreadLocal.set(SoftReference(obj))
            }
        }
        return obj
    }

    init {
        mThreadLocal = ThreadLocal()
    }
}

internal class CanvasCache : SoftReferenceThreadLocal<Canvas>() {
    override fun initialValue(): Canvas {
        return Canvas()
    }
}

internal class PaintCache : SoftReferenceThreadLocal<Paint?>() {
    override fun initialValue(): Paint? {
        return null
    }
}

internal class BitmapCache : SoftReferenceThreadLocal<Bitmap?>() {
    override fun initialValue(): Bitmap? {
        return null
    }
}

internal class RectCache : SoftReferenceThreadLocal<Rect>() {
    override fun initialValue(): Rect {
        return Rect()
    }
}

internal class BitmapFactoryOptionsCache : SoftReferenceThreadLocal<BitmapFactory.Options>() {
    override fun initialValue(): BitmapFactory.Options {
        return BitmapFactory.Options()
    }
}

class WidgetPreviewLoader(private val mLauncher: SamSprungPanels) {
    private var mPreviewBitmapWidth = 0
    private var mPreviewBitmapHeight = 0
    private var mSize: String? = null
    private val mContext: Context
    private val mPackageManager: PackageManager

    // Used for drawing shortcut previews
    private val mCachedShortcutPreviewBitmap = BitmapCache()
    private val mCachedShortcutPreviewPaint = PaintCache()
    private val mCachedShortcutPreviewCanvas = CanvasCache()

    // Used for drawing widget previews
    private val mCachedAppWidgetPreviewCanvas = CanvasCache()
    private val mCachedAppWidgetPreviewSrcRect = RectCache()
    private val mCachedAppWidgetPreviewDestRect = RectCache()
    private val mCachedAppWidgetPreviewPaint = PaintCache()
    private var mCachedSelectQuery: String? = null
    private val mCachedBitmapFactoryOptions = BitmapFactoryOptionsCache()
    private val mAppIconSize: Int
    private val mProfileBadgeSize: Int
    private val mProfileBadgeMargin: Int
    private var mDb: CacheDb?
    private val mLoadedPreviews: HashMap<String, WeakReference<Bitmap?>>
    private val mUnusedBitmaps: ArrayList<SoftReference<Bitmap?>>

    companion object {
        const val TAG = "WidgetPreviewLoader"
        const val ANDROID_INCREMENTAL_VERSION_NAME_KEY = "android.incremental.version"
        private var sInvalidPackages: HashSet<String> = hashSetOf()
        private const val WIDGET_PREFIX = "Widget:"
        private const val SHORTCUT_PREFIX = "Shortcut:"
        private fun getObjectName(o: Any): String {
            // should cache the string builder
            val sb = StringBuilder()
            val output: String
            if (o is AppWidgetProviderInfo) {
                val info = o
                sb.append(WIDGET_PREFIX)
                sb.append(info.profile)
                sb.append('/')
                sb.append(info.provider.flattenToString())
            } else {
                sb.append(SHORTCUT_PREFIX)
                val info = o as ResolveInfo
                sb.append(
                    ComponentName(
                        info.activityInfo.packageName,
                        info.activityInfo.name
                    ).flattenToString()
                )
            }
            output = sb.toString()
            sb.setLength(0)
            return output
        }

        fun removeFromDb(cacheDb: CacheDb, packageName: String) {
            synchronized(sInvalidPackages) { sInvalidPackages.add(packageName) }
            Executors.newSingleThreadExecutor().execute {
                val db = cacheDb.writableDatabase
                try {
                    db.delete(
                        CacheDb.TABLE_NAME,
                        CacheDb.COLUMN_NAME + " LIKE ? OR " +
                                CacheDb.COLUMN_NAME + " LIKE ?", arrayOf(
                            "$WIDGET_PREFIX$packageName/%",
                            "$SHORTCUT_PREFIX$packageName/%"
                        )
                    )
                    synchronized(sInvalidPackages) { sInvalidPackages.remove(packageName) }
                } catch (ignored: SQLiteDiskIOException) {
                }
            }
        }

        fun renderDrawableToBitmap(
            d: Drawable?, bitmap: Bitmap?, x: Int, y: Int, w: Int, h: Int
        ) {
            renderDrawableToBitmap(d, bitmap, x, y, w, h, 1f)
        }

        private fun renderDrawableToBitmap(
            d: Drawable?, bitmap: Bitmap?, x: Int, y: Int, w: Int, h: Int, scale: Float
        ) {
            if (bitmap != null) {
                val c = Canvas(bitmap)
                c.scale(scale, scale)
                val oldBounds = d!!.copyBounds()
                d.setBounds(x, y, x + w, y + h)
                d.draw(c)
                d.bounds = oldBounds // Restore the bounds
                c.setBitmap(null)
            }
        }

        init {
            sInvalidPackages = HashSet()
        }
    }

    private fun recreateDb() {
        val app = mLauncher.application as SamSprung
        app.recreateWidgetPreviewDb()
        mDb = app.getWidgetPreviewCacheDb()
    }

    fun setPreviewSize(previewWidth: Int, previewHeight: Int) {
        mPreviewBitmapWidth = previewWidth
        mPreviewBitmapHeight = previewHeight
        mSize = previewWidth.toString() + "x" + previewHeight
    }

    fun getPreview(o: Any): Bitmap? {
        val name = getObjectName(o)
        // check if the package is valid
        var packageValid = true
        synchronized(sInvalidPackages) {
            packageValid = !sInvalidPackages.contains(getObjectPackage(o))
        }
        if (!packageValid) {
            return null
        }
        synchronized(mLoadedPreviews) {
            // check if it exists in our existing cache
            if (mLoadedPreviews.containsKey(name) && mLoadedPreviews[name]!!.get() != null) {
                return mLoadedPreviews[name]!!.get()
            }
        }
        var unusedBitmap: Bitmap? = null
        synchronized(mUnusedBitmaps) {

            // not in cache; we need to load it from the db
            while ((unusedBitmap == null || !unusedBitmap!!.isMutable || unusedBitmap!!.width != mPreviewBitmapWidth || unusedBitmap!!.height != mPreviewBitmapHeight)
                && mUnusedBitmaps.size > 0
            ) {
                unusedBitmap = mUnusedBitmaps.removeAt(0).get()
            }
            if (unusedBitmap != null) {
                val c = mCachedAppWidgetPreviewCanvas.get()!!
                c.setBitmap(unusedBitmap)
                c.drawColor(0, PorterDuff.Mode.CLEAR)
                c.setBitmap(null)
            }
        }
        if (unusedBitmap == null) {
            unusedBitmap = Bitmap.createBitmap(
                mPreviewBitmapWidth, mPreviewBitmapHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        var preview: Bitmap? = null
        preview = readFromDb(name, unusedBitmap)
        if (preview != null) {
            synchronized(mLoadedPreviews) { mLoadedPreviews.put(name, WeakReference(preview)) }
        } else {
            // it's not in the db... we need to generate it
            val generatedPreview = generatePreview(o, unusedBitmap)
            preview = generatedPreview
            if (preview != unusedBitmap) {
                throw RuntimeException("generatePreview is not recycling the bitmap $o")
            }
            synchronized(mLoadedPreviews) { mLoadedPreviews.put(name, WeakReference(preview)) }

            // write to db on a thread pool... this can be done lazily and improves the performance
            // of the first time widget previews are loaded
            Executors.newSingleThreadExecutor().execute { writeToDb(o, generatedPreview) }
        }
        return preview
    }

    fun recycleBitmap(o: Any?, bitmapToRecycle: Bitmap) {
        val name = getObjectName(o!!)
        synchronized(mLoadedPreviews) {
            if (mLoadedPreviews.containsKey(name)) {
                val b = mLoadedPreviews[name]!!.get()
                if (b == bitmapToRecycle) {
                    mLoadedPreviews.remove(name)
                    if (bitmapToRecycle.isMutable) {
                        synchronized(mUnusedBitmaps) {
                            mUnusedBitmaps.add(
                                SoftReference(
                                    b
                                )
                            )
                        }
                    }
                } else {
                    throw java.lang.RuntimeException("Bitmap passed in doesn't match up")
                }
            }
        }
    }

    class CacheDb(mContext: Context) : SQLiteOpenHelper(
        mContext, File(mContext.cacheDir, DB_NAME).path, null, DB_VERSION
    ) {
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        COLUMN_NAME + " TEXT NOT NULL, " +
                        COLUMN_SIZE + " TEXT NOT NULL, " +
                        COLUMN_PREVIEW_BITMAP + " BLOB NOT NULL, " +
                        "PRIMARY KEY (" + COLUMN_NAME + ", " + COLUMN_SIZE + ") " +
                        ");"
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion != newVersion) {
                // Delete all the records; they'll be repopulated as this is a cache
                db.execSQL("DELETE FROM " + TABLE_NAME)
            }
        }

        companion object {
            val DB_VERSION = 2
            val DB_NAME = "widgetpreviews.db"
            val TABLE_NAME = "shortcut_and_widget_previews"
            val COLUMN_NAME = "name"
            val COLUMN_SIZE = "size"
            val COLUMN_PREVIEW_BITMAP = "preview_bitmap"
        }
    }

    private fun getObjectPackage(o: Any): String {
        return if (o is AppWidgetProviderInfo) {
            o.provider.packageName
        } else {
            val info = o as ResolveInfo
            info.activityInfo.packageName
        }
    }

    private fun writeToDb(o: Any, preview: Bitmap?) {
        val name = getObjectName(o)
        val db = mDb!!.writableDatabase
        val values = ContentValues()
        values.put(CacheDb.COLUMN_NAME, name)
        val stream = ByteArrayOutputStream()
        preview!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
        values.put(CacheDb.COLUMN_PREVIEW_BITMAP, stream.toByteArray())
        values.put(CacheDb.COLUMN_SIZE, mSize)
        try {
            db.insert(CacheDb.TABLE_NAME, null, values)
        } catch (e: SQLiteDiskIOException) {
            recreateDb()
        }
    }

    private fun clearDb() {
        val db = mDb!!.writableDatabase
        // Delete everything
        try {
            db.delete(CacheDb.TABLE_NAME, null, null)
        } catch (ignored: SQLiteDiskIOException) {
        }
    }

    private fun readFromDb(name: String, b: Bitmap?): Bitmap? {
        if (mCachedSelectQuery == null) {
            mCachedSelectQuery = CacheDb.COLUMN_NAME + " = ? AND " +
                    CacheDb.COLUMN_SIZE + " = ?"
        }
        val db = mDb!!.readableDatabase
        val result: Cursor
        result = try {
            db.query(
                CacheDb.TABLE_NAME, arrayOf(CacheDb.COLUMN_PREVIEW_BITMAP),  // cols to return
                mCachedSelectQuery, arrayOf(name, mSize),  // args to select query
                null,
                null,
                null,
                null
            )
        } catch (e: SQLiteDiskIOException) {
            recreateDb()
            return null
        }
        return if (result.count > 0) {
            result.moveToFirst()
            val blob = result.getBlob(0)
            result.close()
            val opts = mCachedBitmapFactoryOptions.get()!!
            opts.inBitmap = b
            opts.inSampleSize = 1
            BitmapFactory.decodeByteArray(blob, 0, blob.size, opts)
        } else {
            result.close()
            null
        }
    }

    fun generatePreview(info: Any, preview: Bitmap?): Bitmap? {
        if (preview != null &&
            (preview.width != mPreviewBitmapWidth ||
                    preview.height != mPreviewBitmapHeight)
        ) {
            throw RuntimeException("Improperly sized bitmap passed as argument")
        }
        return if (info is AppWidgetProviderInfo) {
            generateWidgetPreview(info, preview)
        } else {
            generateShortcutPreview(
                info as ResolveInfo, mPreviewBitmapWidth, mPreviewBitmapHeight, preview
            )
        }
    }

    fun generateWidgetPreview(info: AppWidgetProviderInfo, preview: Bitmap?): Bitmap? {
        val cellSpans = mLauncher.getWidgetMaxSize(info)
        val maxWidth = maxWidthForWidgetPreview(cellSpans[0])
        val maxHeight = maxHeightForWidgetPreview(cellSpans[1])
        return generateWidgetPreview(info, maxWidth, maxHeight, preview, null)
    }

    fun maxWidthForWidgetPreview(spanX: Int): Int {
        return Math.min(mPreviewBitmapWidth, spanX)
    }

    fun maxHeightForWidgetPreview(spanY: Int): Int {
        return Math.min(mPreviewBitmapHeight, spanY)
    }

    fun generateWidgetPreview(
        info: AppWidgetProviderInfo,
        maxPreviewWidth: Int, maxPreviewHeight: Int, preview: Bitmap?,
        preScaledWidthOut: IntArray?
    ): Bitmap? {
        // Load the preview image if possible
        var maxPreviewWidth = maxPreviewWidth
        var maxPreviewHeight = maxPreviewHeight
        var preview = preview
        if (maxPreviewWidth < 0) maxPreviewWidth = Int.MAX_VALUE
        if (maxPreviewHeight < 0) maxPreviewHeight = Int.MAX_VALUE
        val drawable = info.loadPreviewImage(mContext, 0)
        var previewWidth = 0
        var previewHeight = 0
        var defaultPreview: Bitmap? = null
        val widgetPreviewExists = drawable != null
        if (widgetPreviewExists) {
            previewWidth = drawable!!.intrinsicWidth
            previewHeight = drawable.intrinsicHeight
        } else {
            // Generate a preview image if we couldn't load one
            val previewDrawable = ResourcesCompat.getDrawable(
                mContext.resources, R.drawable.widget_preview_tile, mContext.theme
            ) as BitmapDrawable?
            if (null != previewDrawable) {
                val previewDrawableWidth = previewDrawable.intrinsicWidth
                val previewDrawableHeight = previewDrawable.intrinsicHeight
                previewWidth = previewDrawableWidth // subtract 2 dips
                previewHeight = previewDrawableHeight
                defaultPreview = Bitmap.createBitmap(
                    previewWidth, previewHeight,
                    Bitmap.Config.ARGB_8888
                )
                val c = mCachedAppWidgetPreviewCanvas.get()!!
                c.setBitmap(defaultPreview)
                previewDrawable.setBounds(0, 0, previewWidth, previewHeight)
                previewDrawable.setTileModeXY(
                    Shader.TileMode.REPEAT,
                    Shader.TileMode.REPEAT
                )
                previewDrawable.draw(c)
                c.setBitmap(null)

                // Draw the icon in the top left corner
                val sWidgetPreviewIconPaddingPercentage = 0.25f
                val minOffset = (mAppIconSize * sWidgetPreviewIconPaddingPercentage).toInt()
                val smallestSide = Math.min(previewWidth, previewHeight)
                val iconScale = Math.min(
                    smallestSide.toFloat()
                            / (mAppIconSize + 2 * minOffset), 1f
                )
                try {
                    var icon: Drawable? = null
                    val hoffset = ((previewDrawableWidth - mAppIconSize * iconScale) / 2).toInt()
                    val yoffset = ((previewDrawableHeight - mAppIconSize * iconScale) / 2).toInt()
                    if (info.icon > 0) icon = getFullResIcon(
                        info.provider.packageName,
                        info.icon, info.profile
                    )
                    if (icon != null) {
                        renderDrawableToBitmap(
                            icon, defaultPreview, hoffset,
                            yoffset, (mAppIconSize * iconScale).toInt(),
                            (mAppIconSize * iconScale).toInt()
                        )
                    }
                } catch (ignored: NotFoundException) {
                }
            }
        }

        // Scale to fit width only - let the widget preview be clipped in the
        // vertical dimension
        var scale = 1f
        if (preScaledWidthOut != null) {
            preScaledWidthOut[0] = previewWidth
        }
        if (previewWidth > maxPreviewWidth) {
            scale = maxPreviewWidth / previewWidth.toFloat()
        }
        if (scale != 1f) {
            previewWidth = (scale * previewWidth).toInt()
            previewHeight = (scale * previewHeight).toInt()
        }

        // If a bitmap is passed in, we use it; otherwise, we create a bitmap of the right size
        if (preview == null) {
            preview = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        }

        // Draw the scaled preview into the final bitmap
        val x = (preview!!.width - previewWidth) / 2
        if (widgetPreviewExists) {
            renderDrawableToBitmap(
                drawable, preview, x, 0, previewWidth,
                previewHeight
            )
        } else {
            val c = mCachedAppWidgetPreviewCanvas.get()!!
            val src = mCachedAppWidgetPreviewSrcRect.get()!!
            val dest = mCachedAppWidgetPreviewDestRect.get()!!
            c.setBitmap(preview)
            if (null != defaultPreview) src[0, 0, defaultPreview.width] = defaultPreview.height
            dest[x, 0, x + previewWidth] = previewHeight
            var p = mCachedAppWidgetPreviewPaint.get()
            if (p == null) {
                p = Paint()
                p.isFilterBitmap = true
                mCachedAppWidgetPreviewPaint.set(p)
            }
            c.drawBitmap(defaultPreview!!, src, dest, p)
            c.setBitmap(null)
        }

        // Finally, if the preview is for a managed profile, badge it.
        if (info.profile != Process.myUserHandle()) {
            val previewBitmapWidth = preview.width
            val previewBitmapHeight = preview.height

            // Figure out the badge location.
            val badgeLocation: Rect
            val configuration = mContext.resources.configuration
            badgeLocation = if (configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR) {
                val badgeLeft = previewBitmapWidth - mProfileBadgeSize - mProfileBadgeMargin
                val badgeTop = previewBitmapHeight - mProfileBadgeSize - mProfileBadgeMargin
                val badgeRight = badgeLeft + mProfileBadgeSize
                val badgeBottom = badgeTop + mProfileBadgeSize
                Rect(badgeLeft, badgeTop, badgeRight, badgeBottom)
            } else {
                val badgeLeft = mProfileBadgeMargin
                val badgeTop = previewBitmapHeight - mProfileBadgeSize - mProfileBadgeMargin
                val badgeRight = badgeLeft + mProfileBadgeSize
                val badgeBottom = badgeTop + mProfileBadgeSize
                Rect(badgeLeft, badgeTop, badgeRight, badgeBottom)
            }

            // Badge the preview.
            val previewDrawable = BitmapDrawable(
                mContext.resources, preview
            )
            val badgedPreviewDrawable = mContext.packageManager.getUserBadgedDrawableForDensity(
                previewDrawable, info.profile, badgeLocation, 0
            )

            // Return the badged bitmap.
            if (badgedPreviewDrawable is BitmapDrawable) {
                return badgedPreviewDrawable.bitmap
            }
        }
        return preview
    }

    val fullResDefaultActivityIcon: Drawable
        get() = getFullResIcon(
            Resources.getSystem(),
            android.R.mipmap.sym_def_app_icon, Process.myUserHandle()
        )

    fun getFullResIcon(resources: Resources, iconId: Int, user: UserHandle?): Drawable {
        var d: Drawable?
        d = try {
            ResourcesCompat.getDrawableForDensity(resources, iconId, 160, resources.newTheme())
        } catch (e: NotFoundException) {
            null
        }
        if (d == null) {
            d = fullResDefaultActivityIcon
        }
        return mPackageManager.getUserBadgedIcon(d, user!!)
    }

    fun getFullResIcon(packageName: String?, iconId: Int, user: UserHandle?): Drawable {
        val resources: Resources?
        resources = try {
            // TODO: Check if this needs to use the user param if we support
            // shortcuts/widgets from other profiles. It won't work as is
            // for packages that are only available in a different user profile.
            mPackageManager.getResourcesForApplication(packageName!!)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        if (resources != null) {
            if (iconId != 0) {
                return getFullResIcon(resources, iconId, user)
            }
        }
        return fullResDefaultActivityIcon
    }

    fun getFullResIcon(info: ResolveInfo, user: UserHandle?): Drawable {
        return getFullResIcon(info.activityInfo, user)
    }

    fun getFullResIcon(info: ActivityInfo, user: UserHandle?): Drawable {
        val resources: Resources?
        resources = try {
            mPackageManager.getResourcesForApplication(
                info.applicationInfo
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        if (resources != null) {
            val iconId = info.iconResource
            if (iconId != 0) {
                return getFullResIcon(resources, iconId, user)
            }
        }
        return fullResDefaultActivityIcon
    }

    private fun generateShortcutPreview(
        info: ResolveInfo, maxWidth: Int, maxHeight: Int, preview: Bitmap?
    ): Bitmap? {
        var preview = preview
        var tempBitmap = mCachedShortcutPreviewBitmap.get()!!
        val c = mCachedShortcutPreviewCanvas.get()!!
        if (tempBitmap == null || tempBitmap.width != maxWidth || tempBitmap.height != maxHeight) {
            tempBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
            mCachedShortcutPreviewBitmap.set(tempBitmap)
        } else {
            c.setBitmap(tempBitmap)
            c.drawColor(0, PorterDuff.Mode.CLEAR)
            c.setBitmap(null)
        }
        // Render the icon
        val icon = getFullResIcon(info, Process.myUserHandle())
        renderDrawableToBitmap(icon, tempBitmap, 0, 0, maxWidth, maxWidth)
        if (preview != null &&
            (preview.width != maxWidth || preview.height != maxHeight)
        ) {
            throw RuntimeException("Improperly sized bitmap passed as argument")
        } else if (preview == null) {
            preview = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
        }
        c.setBitmap(preview)
        // Draw a desaturated/scaled version of the icon in the background as a watermark
        var p = mCachedShortcutPreviewPaint.get()
        if (p == null) {
            p = Paint()
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            p.colorFilter = ColorMatrixColorFilter(colorMatrix)
            p.alpha = (255 * 0.06f).toInt()
            mCachedShortcutPreviewPaint.set(p)
        }
        c.drawBitmap(tempBitmap, 0f, 0f, p)
        c.setBitmap(null)
        renderDrawableToBitmap(icon, preview, 0, 0, mAppIconSize, mAppIconSize)
        return preview
    }

    init {
        mContext = mLauncher
        mPackageManager = mContext.packageManager
        mAppIconSize = mContext.resources.getDimensionPixelSize(R.dimen.app_icon_size)
        mProfileBadgeSize = mContext.resources.getDimensionPixelSize(
            R.dimen.profile_badge_size
        )
        mProfileBadgeMargin = mContext.resources.getDimensionPixelSize(
            R.dimen.profile_badge_margin
        )
        val app = mLauncher.applicationContext as SamSprung
        mDb = app.getWidgetPreviewCacheDb()
        mLoadedPreviews = HashMap()
        mUnusedBitmaps = ArrayList()
        val sp = mLauncher.getSharedPreferences(
            SamSprung.prefsValue, Context.MODE_PRIVATE
        )
        val lastVersionName = sp.getString(ANDROID_INCREMENTAL_VERSION_NAME_KEY, null)
        val versionName = Build.VERSION.INCREMENTAL
        if (versionName != lastVersionName) {
            // clear all the previews whenever the system version changes, to ensure that previews
            // are up-to-date for any apps that might have been updated with the system
            clearDb()
            val editor = sp.edit()
            editor.putString(ANDROID_INCREMENTAL_VERSION_NAME_KEY, versionName)
            editor.apply()
        }
    }
}