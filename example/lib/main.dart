import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:android_app_info/android_app_info.dart';

void main() {
  runApp(new MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  List<AndroidAppInfo> _apps = [];

  @override
  initState() {
    super.initState();
    initPlatformState();
  }

  initPlatformState() async {
    String platformVersion;
    try {
      platformVersion = await AndroidAppInfo.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // THIS IS THE MAGIC
    // Grabs the list of avaiable application activities.
    // If you want the actual *applications*, try AndroidAppInfo.getInstalledApplications.
    // (They both return Future<List<AndroidAppInfo>>).
    var apps = await AndroidAppInfo.getAvailableActivities();

    if (!mounted)
      return;

    setState(() {
      _platformVersion = platformVersion;
      _apps = apps;
    });
  }

  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: new Text('Plugin example app'),
        ),
        body: new ListView.builder(
          itemCount: _apps.length,
          itemBuilder: (BuildContext context, int index) {
            var app = _apps[index];
            return new ListTile(
              // For a full list of the properties on an AndroidAppInfo instance, see the source
              // app.icon is the app's icon
              // app.defaultIcon is the default application icon
              leading: new Image.memory(app.icon ?? app.defaultIcon),
              // obvious...
              title: new Text(app.label ?? app.name ?? 'Unknown'),
              // also obvious...
              subtitle: new Text('${app.packageName}'),
              onTap: () => app.start(),
            );
          },
        ),
      ),
    );
  }
}
