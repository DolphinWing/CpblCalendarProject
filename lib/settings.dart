import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show rootBundle;
import 'package:package_info/package_info.dart';

import 'content.dart';
import 'cpbl.dart';
import 'lang.dart';

class SettingsPane extends StatefulWidget {
  final CpblClient client;

  SettingsPane({this.client});

  @override
  State<StatefulWidget> createState() => _SettingsPaneState();
}

class _SettingsPaneState extends State<SettingsPane> {
  PackageInfo _packageInfo = new PackageInfo(
    appName: 'Unknown',
    packageName: 'Unknown',
    version: 'Unknown',
    buildNumber: 'Unknown',
  );
  bool _enableViewPager = false;
  bool _enableHighlight = false;

  @override
  void initState() {
    super.initState();
    prepare();
  }

  Future<void> prepare() async {
    final PackageInfo info = await PackageInfo.fromPlatform();
    setState(() {
      _enableViewPager = widget.client?.isViewPagerEnabled() ?? false;
      _enableHighlight = widget.client?.isHighlightEnabled() ?? false;
      _packageInfo = info;
    });
  }

  showDataInDialog(String data) => showDialog(
      context: context,
      builder: (context) => ChipMenuDialog('', [
            Padding(
              child: Text(data),
              padding: EdgeInsets.only(left: 16, right: 16),
            )
          ]));

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Scaffold(
        appBar: AppBar(
          title: Text(Lang.of(context).trans('action_settings')),
          centerTitle: false,
        ),
        body: ListView(
          children: <Widget>[
            ListTile(
              title: Text(Lang.of(context).trans('app_version')),
              subtitle: Text('${_packageInfo.version} (${_packageInfo.buildNumber})'),
              onTap: () async {
                showDataInDialog(await rootBundle.loadString('assets/strings/version.txt'));
              },
            ),
            SettingsGroupTitle('display_settings'),
            CheckboxListTile(
              title: Text(Lang.of(context).trans('use_view_pager_title')),
              subtitle: Text(
                Lang.of(context).trans(
                    _enableViewPager ? 'use_view_pager_summary_on' : 'use_view_pager_summary_off'),
              ),
              value: _enableViewPager,
              onChanged: (checked) {
                setState(() {
                  _enableViewPager = checked;
                });
                widget.client?.setViewPagerEnabled(checked);
              },
            ),
            //Divider(),
            CheckboxListTile(
              title: Text(Lang.of(context).trans('show_highlight_title')),
              subtitle: Text(
                Lang.of(context).trans(
                    _enableHighlight ? 'show_highlight_summary_on' : 'show_highlight_summary_off'),
              ),
              value: _enableHighlight,
              onChanged: (checked) {
                setState(() {
                  _enableHighlight = checked;
                });
                widget.client?.setHighlightEnabled(checked);
              },
            ),
            SettingsGroupTitle('reference_data_source_title'),
            ListTile(
              title: Text(Lang.of(context).trans('reference_cpbl_official_title')),
              subtitle: Text('${CpblClient.homeUrl}'),
              onTap: () {
                CpblClient.launchUrl(context, CpblClient.homeUrl);
              },
            ),
            SettingsGroupTitle('reference_open_source_title'),
            ListTile(
              title: Text('Flutter - Beautiful native apps in record time'),
              onTap: () async {
                showDataInDialog(await rootBundle.loadString('assets/licenses/flutter.txt'));
              },
            ),
            ListTile(
              title: Text('Flutter plugins'),
              onTap: () async {
                showDataInDialog(
                    await rootBundle.loadString('assets/licenses/flutter_plugins.txt'));
              },
            ),
            ListTile(
              title: Text('flutter_custom_tabs'),
              onTap: () async {
                showDataInDialog(
                    await rootBundle.loadString('assets/licenses/flutter_custom_tabs.txt'));
              },
            ),
            ListTile(
              title: Text('fluttertoast'),
              onTap: () async {
                showDataInDialog(await rootBundle.loadString('assets/licenses/flutter_toast.txt'));
              },
            ),
          ],
        ),
      ),
    );
  }
}

class SettingsGroupTitle extends StatelessWidget {
  final String titleResKey;

  SettingsGroupTitle(String titleResKey) : this.titleResKey = titleResKey ?? '';

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Text(
        Lang.of(context).trans(titleResKey),
        style: Theme.of(context).textTheme.caption,
      ),
      constraints: BoxConstraints.expand(height: 36),
      padding: EdgeInsets.fromLTRB(16, 0, 8, 8),
      //color: Colors.grey.withAlpha(32),
      alignment: Alignment.bottomLeft,
    );
  }
}
