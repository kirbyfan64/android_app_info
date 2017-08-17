package com.refi64.androidappinfo

import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.PluginRegistry.Registrar

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.content.pm.PackageItemInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.app.Activity
import android.util.Log

import java.io.ByteArrayOutputStream

class AndroidAppInfoPlugin(activity: Activity): MethodCallHandler {
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar): Unit {
      val channel = MethodChannel(registrar.messenger(), "android_app_info")
      channel.setMethodCallHandler(AndroidAppInfoPlugin(registrar.activity()))
    }
  }

  var ctx = activity
  val pm = activity.packageManager
  val flags = PackageManager.GET_META_DATA

  fun drawableToBitmap(drawable: Drawable): Bitmap {
    // Based on https://stackoverflow.com/a/9390776/2097780

    if (drawable is BitmapDrawable) {
      return drawable.bitmap
    }

    var width = drawable.intrinsicWidth
    var height = drawable.intrinsicHeight

    var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    var canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
  }

  fun loadTextResource(res: Resources, id: Int): Object? {
    if (id == 0) {
      return null
    }

    return res.getString(id) as Object?
  }

  fun loadDrawable(d: Drawable): Object? {
    var bitmap = drawableToBitmap(d)
    var os = ByteArrayOutputStream()

    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)) {
      return null
    }

    return os.toByteArray() as Object?
  }

  fun loadDrawableResource(res: Resources, id: Int): Object? {
    if (id == 0) {
      return null
    }

    return loadDrawable(res.getDrawable(id))
  }

  fun combineMaps(a: Map<String, Object?>, b: Map<String, Object?>):
    Map<String, Object?> {
    var res = mutableMapOf<String, Object?>()
    res.putAll(a)
    res.putAll(b)
    return res
  }

  fun convertPkgInfoToMap(info: PackageItemInfo): Map<String, Object?> {
    var hasLaunchIntent = pm.getLaunchIntentForPackage(info.packageName) != null

    return mapOf(
      "name" to info.name as Object?,
      "packageName" to info.packageName as Object?,
      "defaultIcon" to loadDrawableResource(Resources.getSystem(),
                                            android.R.mipmap.sym_def_app_icon),
      "hasLaunchIntent" to hasLaunchIntent as Object?
    )
  }

  fun convertAppInfoToMap(info: ApplicationInfo): Map<String, Object?> {
    var res = pm.getResourcesForApplication(info.packageName)

    return combineMaps(convertPkgInfoToMap(info), mapOf(
      "className" to info.className as Object?,
      "dataDir" to info.dataDir as Object?,
      "icon" to loadDrawableResource(res, info.icon),
      "label" to pm.getApplicationLabel(info) as Object?
    ))
  }

  fun convertResolveInfoToMap(info: ResolveInfo): Map<String, Object?> {
    var appinfo = pm.getApplicationInfo(info.activityInfo.packageName, flags)

    return combineMaps(convertPkgInfoToMap(info.activityInfo), mapOf(
      "className" to appinfo.className as Object?,
      "dataDir" to appinfo.dataDir as Object?,
      "icon" to loadDrawable(info.loadIcon(pm)),
      "label" to info.loadLabel(pm) as Object?
    ))
  }

  override fun onMethodCall(call: MethodCall, result: Result): Unit {
    try {
      if (call.method.equals("platform-version")) {
        result.success("Android ${android.os.Build.VERSION.RELEASE}")
      } else if (call.method.equals("get-data")) {
        var name: String = call.argument("name")
        var info = pm.getApplicationInfo(name, flags)

        result.success(convertAppInfoToMap(info))
      } else if (call.method.equals("get-apps")) {
        var apps = pm.getInstalledApplications(flags)
        result.success(apps.map { app -> convertAppInfoToMap(app) })
      } else if (call.method.equals("get-activities")) {
        var intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        var activities = pm.queryIntentActivities(intent, 0)
        result.success(activities.map { ri -> convertResolveInfoToMap(ri) })
      } else if (call.method.equals("start-app")) {
        var name: String = call.argument("name")

        var intent = pm.getLaunchIntentForPackage(name)
        ctx.startActivity(intent)
        result.success(null)
      } else {
        result.notImplemented()
      }
    } catch (tr: Throwable) {
      Log.e("android_app_info", "fatal error in onMethodCall", tr)
      result.error("UNKNOWN", "unknown error; see logcat for traceback", tr.toString())
    }
  }
}
