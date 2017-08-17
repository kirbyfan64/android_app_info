import 'package:flutter/services.dart';

import 'dart:async';
import 'dart:typed_data';

class AndroidAppInfo {
  static const MethodChannel _channel =
      const MethodChannel('android_app_info');

  static Future<String> get platformVersion =>
      _channel.invokeMethod('platform-version');

  AndroidAppInfo._internal(data):
    name = data['name'],
    packageName = data['packageName'],
    className = data['className'],
    dataDir = data['dataDir'],
    label = data['label'],

    icon = data['icon'],
    defaultIcon = data['defaultIcon'],

    hasLaunchIntent = data['hasLaunchIntent'];

  String toString() =>
    'AndroidAppInfo[packageName: $packageName]';

  Future start() => _channel.invokeMethod('start-app', {"name": packageName});

  static Future<AndroidAppInfo> getApplication(String name) =>
    new AndroidAppInfo._internal(
      await _channel.invokeMethod('get-data', {"name": name}));

  static Future<List<AndroidAppInfo>> getInstalledApplications() async {
    var res = <AndroidAppInfo>[];
    var apps = await _channel.invokeMethod('get-apps');
    for (var appData in apps) {
      res.add(new AndroidAppInfo._internal(appData));
    }
    return res;
  }

  static Future<List<AndroidAppInfo>> getAvailableActivities() async {
    var res = <AndroidAppInfo>[];
    var activities = await _channel.invokeMethod('get-activities');
    for (var activityData in activities) {
      res.add(new AndroidAppInfo._internal(activityData));
    }
    return res;
  }

  final String name, className, packageName, dataDir, label;
  Uint8List icon, defaultIcon;
  bool hasLaunchIntent;
}
