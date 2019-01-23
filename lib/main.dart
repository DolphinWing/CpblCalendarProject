import 'dart:async';
import 'dart:io';

import 'package:firebase_admob/firebase_admob.dart';
import 'package:firebase_remote_config/firebase_remote_config.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show rootBundle;
import 'package:flutter\_localizations/flutter\_localizations.dart';
import 'package:package_info/package_info.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'content.dart';
import 'cpbl.dart';
import 'drawer.dart';
import 'lang.dart';
//import 'package:url_launcher/url_launcher.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      //title: Lang.of(context).trans('app_name'),
      theme: ThemeData(
        primarySwatch: Colors.lightGreen,
        primaryColor: Colors.lightGreen[800],
        //accentColor: Colors.orange,
      ),
      supportedLocales: [
        //const Locale('en', 'US'),
        const Locale('zh', 'TW'),
      ],
      localizationsDelegates: [
        const LangLocalizationsDelegate(),
        const FallbackCupertinoLocalisationsDelegate(),
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate
      ],
      localeResolutionCallback: (Locale locale, Iterable<Locale> supportedLocales) {
        //print('${locale.languageCode} ${locale.countryCode}');
        for (Locale supportedLocale in supportedLocales) {
          if (supportedLocale.languageCode == locale.languageCode ||
              supportedLocale.countryCode == locale.countryCode) {
            return supportedLocale;
          }
        }
        return supportedLocales.first;
      },
      home: SplashScreen(),
      routes: {
        '/calendar': (context) => MainUiWidget(debug: false),
        '/settings': (context) => SettingsPane(),
      },
    );
  }
}

/// https://medium.com/@vignesh_prakash/flutter-splash-screen-84fb0307ac55
class SplashScreen extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  RemoteConfig configs;
  Timer _timer;

//  String appName = '';
  String versionCode = '';

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Container(
        color: Colors.white,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Image(
              image: AssetImage('assets/drawable/splash_icon.png'),
            ),
            SizedBox(height: 64),
            Container(
              constraints: BoxConstraints(minHeight: 32),
              //child: Text(appName, style: Theme.of(context).textTheme.title),
            ),
            SizedBox(height: 32),
            CircularProgressIndicator(),
            SizedBox(height: 32),
            Container(
              constraints: BoxConstraints(minHeight: 32),
              child: Text(versionCode, style: Theme.of(context).textTheme.caption),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void initState() {
    super.initState();
    _timer = new Timer(const Duration(milliseconds: 500), _initUi);
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _initUi() {
    FirebaseAdMob.instance.initialize(appId: 'ca-app-pub-7557398389502445~8502463329');
    PackageInfo.fromPlatform().then((packageInfo) {
      setState(() {
        versionCode = packageInfo?.buildNumber ?? '';
      });
      navigationPage(int.parse(packageInfo?.buildNumber ?? '0'));
    });
  }

  void navigationPage([int versionCode = 0]) async {
    configs = await RemoteConfig.instance;
    final defaults = <String, dynamic>{'override_start_enabled': false};
    if (CpblClient.debug) {
      await configs.setConfigSettings(new RemoteConfigSettings(debugMode: true));
    }
    await configs.setDefaults(defaults);
    if (CpblClient.debug) {
      await configs.fetch(expiration: const Duration(minutes: 5));
    } else {
      await configs.fetch(expiration: const Duration(hours: 8));
    }
    await configs.activateFetched();
    //print('welcome message: ${configs.getString('welcome')}');
    //print('override: ${configs.getBool('override_start_enabled')}');
    final prefs = await SharedPreferences.getInstance();

    //load large json file in localizationsDelegates will cause black screen
    await Lang.of(context).load(); //late load
//    setState(() {
//      appName = Lang.of(context).trans('app_name');
//    });
    final CpblClient client = new CpblClient(context, configs, prefs, versionCode: versionCode);
    client.init().then((r) {
      Navigator.of(context)
          .pushReplacement(MaterialPageRoute(builder: (context) => MainUiWidget(client: client)));
    }); //we need to init some lang strings
    //Navigator.of(context).pushReplacementNamed('/calendar');
  }
}

class MainUiWidget extends StatefulWidget {
  final bool debug;
  final CpblClient client;

  MainUiWidget({this.debug = false, this.client});

  @override
  State<StatefulWidget> createState() =>
      client.isViewPagerEnabled() ? _MainUi2WidgetState() : _MainUiWidgetState();
}

class _MainUiWidgetState extends State<MainUiWidget> {
  //https://www.reddit.com/r/FlutterDev/comments/7yma7y/how_do_you_open_a_drawer_in_a_scaffold_using_code/duhllqz
  GlobalKey<ScaffoldState> _scaffoldKey = new GlobalKey();
  Duration _fetchDelay = const Duration(milliseconds: 400);

  Timer _timer;
  CpblClient _client;

  bool loading = false;
  List<Game> list;
  bool showFab = false;
  int _year;
  int _month;
  GameType _type;
  int _mode = UiMode.list;
  FieldId _field;
  TeamId _favTeam;
  List<FieldId> _fieldList;
  List<TeamId> _teamList;

  @override
  void initState() {
    super.initState();

//    //FIXME: auto select year and month
//    int year = 2018;
//    int month = 10;
//    setState(() {
//      _year = year;
//      _month = month;
//    });
//    print('init from $_year/$_month');

    /// Flutter get context in initState method
    /// https://stackoverflow.com/a/49458289/2673859
    new Future.delayed(Duration.zero, () async {
      if (widget.client != null) {
        _client = widget.client;
        //already init before
        //if (_client.isOverrideStartEnabled()) {
        setState(() {
          _year = _client.getStartYear();
          _month = _client.getStartMonth();
        });
        //}
        startFetchGames(); //init
      } else {
        //not loaded in splash
        final configs = await RemoteConfig.instance;
//        if (configs.getBool('override_start_enabled')) {
//          year = configs.getInt('override_start_year');
//          month = configs.getInt('override_start_month');
//          print('override: year=$year month=$month');
//        }

        final prefs = await SharedPreferences.getInstance();
        _client = new CpblClient(context, configs, prefs);
        //var now = DateTime.now();
        _client.init().then((value) {
          //if (_client.isOverrideStartEnabled()) {
          setState(() {
            _year = _client.getStartYear();
            _month = _client.getStartMonth();
          });
          //}
          startFetchGames(); //no splash init
        });
      }
    });
  }

  void startFetchGames() {
    print('init from $_year/$_month');
    if (_client.isHighlightEnabled()) {
      fetchHighlight(_year, _month, debug: widget.debug); //init
    } else {
      pullToRefresh(_year, _month, debug: widget.debug); //init
    }
    setState(() {
      showFab = !_client.isHighlightEnabled();
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void pullToRefresh(int year, int month,
      {GameType type = GameType.type_01, bool debug = false, bool forceUpdate = false}) {
    setState(() {
      loading = true;
      _year = year;
      _month = month;
      _type = type;
      _mode = UiMode.list;
      showFab = true;
    });
    if (debug) {
      _timer = new Timer(const Duration(seconds: 2), () {
        setState(() {
          list = new List();
          for (int i = 0; i < 10; i++) {
            list.add(new Game(
                id: i + 1,
                home: Team.simple(TeamId.values[i % TeamId.values.length], true),
                away: Team.simple(TeamId.values[(i + 10) % TeamId.values.length], false),
                fieldId: FieldId.values[i % FieldId.values.length]));
          }
          loading = false;
        });
      });
    } else {
      _startTimeout(_timeoutRefresh); //start fetch game list
      _client.fetchList(year, month, type, !forceUpdate).then((gameList) {
        _clearTimeout(); //fetch game list complete
        List<FieldId> fields = [FieldId.f00];
        List<TeamId> teams = [TeamId.fav_all];
        list?.forEach((g) {
          if (!fields.contains(g.fieldId)) fields.add(g.fieldId);
          if (!teams.contains(g.home.id)) teams.add(g.home.id);
          if (!teams.contains(g.away.id)) teams.add(g.away.id);
        });
        fields.sort((a, b) => a.index - b.index);
        teams.sort((a, b) => a.index - b.index);
        setState(() {
          _fieldList = fields;
          _teamList = teams;
          list = gameList;
          loading = false;
        });
      });
    }
  }

  void _startTimeout(void callback(), [int seconds = 10]) {
    _clearTimeout();
    _timer = new Timer(new Duration(seconds: seconds), callback);
  }

  void _clearTimeout() {
    _timer?.cancel();
  }

  void _timeoutRefresh() {
    setState(() {
      loading = false;
    });
    refreshGameList();
  }

  Future<List<Game>> _fetchList(int year, int month, GameType type,
      {DateTime time, bool forceUpdate = false}) async {
    List<Game> list = new List();
    var list1 = await _client.fetchList(year, month, type);
    if (list1.isNotEmpty) {
      if (list1.last.before(time)) {
        print('last game is before now');
        list.addAll(list1);
        sleep(_fetchDelay);
        var list2 = await _client.fetchList(year, month + 1, type);
        list.addAll(list2);
      } else if (list1.first.after(time)) {
        print('first game is not yet coming');
        sleep(_fetchDelay);
        list.addAll(await _client.fetchList(year, month - 1, type));
        list.addAll(list1);
      } else {
        print('add ${list1.length} to normal list');
        list.addAll(list1);
      }
    }
    return list;
  }

  void fetchHighlight(int year, int month, {bool debug = false, bool forceUpdate = false}) async {
    setState(() {
      loading = true;
      _mode = UiMode.quick;
      showFab = false;
    });

    List<Game> gameList = new List();
    //load version update
    if (_client.canUpdate()) {
      gameList.add(Game.update(_client.getUpdateSummary()));
    }
    //load remote announcement
    var cards = _client.loadRemoteAnnouncement();
    print('remote info size: ${cards.length}');
    gameList.addAll(cards);
    if (debug) {
      for (int i = 0; i < 3; i++) {
        gameList.add(Game.simple(
          i,
          homeId: TeamId.values[i % TeamId.values.length],
          awayId: TeamId.values[(i + 10) % TeamId.values.length],
        ));
      }
    } else {
      _startTimeout(_timeoutRefresh); //start fetch highlight
      var time = DateTime.now(); //DateTime(2018, 10, 10);
      List<Game> list = new List();
      if (_client.isWarmUpMonth(year, month)) {
        list.addAll(await _client.fetchList(year, month, GameType.type_07)); //warm
        sleep(_fetchDelay);
      }
      list.addAll(await _fetchList(year, month, GameType.type_01, time: time)); //regular
      if (_client.isChallengeMonth(year, month)) {
        list.addAll(await _fetchList(year, month, GameType.type_05, time: time)); //challenge
      }
      if (_client.isChampionMonth(year, month)) {
        list.addAll(await _fetchList(year, month, GameType.type_03, time: time)); //champion
      }
      if (_client.isAllStarMonth(year, month)) {
        list.addAll(await _client.fetchList(year, month, GameType.type_02)); // all star
      }
      _clearTimeout(); //fetch highlight complete
      gameList.addAll(_client.getHighlightGameList(list, time));
    }
    //show more button
    if (gameList.isNotEmpty) {
      gameList.add(Game.more());
    }
    setState(() {
      list = gameList;
      loading = false;
    });
  }

  List<Widget> buildOptionMenu(BuildContext context, bool loading, int year) {
    List<Widget> options = new List();
    if (year == DateTime.now().year) {
      options.add(FlatButton(
        child: Text(
          Lang.of(context).trans('action_lead_board'),
          style: TextStyle(color: Colors.white),
        ),
        onPressed: () {
          CpblClient.launchUrl(context, 'http://www.cpbl.com.tw/standing/season.html');
        },
      ));
    }
    options.addAll([
      IconButton(
        icon: Icon(Icons.refresh),
        onPressed: refreshGameList,
        tooltip: Lang.of(context).trans('action_refresh'),
      ),
      PopupMenuButton(
        itemBuilder: (context) => [
              PopupMenuItem(
                value: 0,
                child: Text(Lang.of(context).trans('action_go_to_website')),
              ),
              PopupMenuItem(
                value: 1,
                child: Text(Lang.of(context).trans('action_settings')),
              ),
//                    PopupMenuItem(
//                      enabled: !loading,
//                      value: 2,
//                      child: Text('show highlight'),
//                    ),
            ],
        onSelected: (action) {
          //print('selected option $action');
          switch (action) {
            case 0:
              showCpblHome();
              break;
            case 1:
              showSettings();
              break;
//            case 2:
//              fetchHighlight(_year, _month, debug: true); //option menu
//              break;
          }
        },
      ),
    ]);
    return options;
  }

  void refreshGameList() {
    if (loading) {
      print('still refresh... bypass it');
    } else if (_mode == UiMode.list) {
      pullToRefresh(_year, _month, type: _type, forceUpdate: true);
    } else {
      fetchHighlight(_year, _month, forceUpdate: true); //refresh button
    }
  }

  void showCpblHome() async {
    //if (await canLaunch(CpblClient.homeUrl)) {
    await CpblClient.launchUrl(context, CpblClient.homeUrl);
    //} else {
    //  throw 'Could not launch ${CpblClient.homeUrl}';
    //}
  }

  void showSettings() async {
    //Navigator.of(context).pushNamed('/settings');
    await Navigator.of(context)
        .push(MaterialPageRoute(builder: (context) => SettingsPane(client: _client)));
    //refresh for current settings
    Navigator.of(context)
        .pushReplacement(MaterialPageRoute(builder: (context) => MainUiWidget(client: _client)));
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Scaffold(
        key: _scaffoldKey,
        appBar: AppBar(
          title: Text(_mode != UiMode.list || list?.isNotEmpty == true
                  ? Lang.of(context).trans('app_name')
                  : '${CpblClient.getGameType(context, (_type ?? GameType.type_01))}'
                  ' ${(_year ?? DateTime.now().year).toString()}'
                  ' ${CpblClient.getMonthString(context, (_month ?? DateTime.now().month))}'
              //Lang.of(context).trans('app_name'),
              //style: TextStyle(color: Colors.white),
              ),
          actions: buildOptionMenu(context, loading, _year),
          //leading: new Container(),
          automaticallyImplyLeading: false,
          elevation: _mode == UiMode.quick ? 0.0 : 4.0,
        ),
        endDrawer: DrawerPane(
          year: _year,
          month: _month,
          type: _type,
          field: _field,
          favTeam: _favTeam,
          fieldList: _fieldList,
          teamList: _teamList,
          onPressed: (action) {
            print('query ${action.year}/${action.month} ${action.field} ${action.type}');
            setState(() {
              _field = action.field;
              _favTeam = action.favTeam;
            });
            pullToRefresh(action.year, action.month, type: action.type);
          },
        ),
        body: Material(
          child: ContentUiWidget(
            loading: loading,
            year: _year,
            month: _month,
            field: _field,
            favTeam: _favTeam,
            mode: _mode,
            list: list,
            onScrollEnd: (value) {
              setState(() {
                showFab = !value && _mode == UiMode.list;
              });
            },
            onHighlightPaneClosed: () {
              pullToRefresh(_year, _month);
            },
          ),
          elevation: 2.0,
        ),
        floatingActionButton: showFab
            ? FloatingActionButton(
                child: Icon(Icons.search),
                onPressed: () {
                  //Scaffold.of(context).openEndDrawer();
                  _scaffoldKey.currentState.openEndDrawer();
                },
                foregroundColor: Colors.white,
                backgroundColor: Theme.of(context).primaryColor,
              )
            : Container(),
      ),
    );
  }
}

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
          ],
        ),
      ),
    );
  }
}

class _MainUi2WidgetState extends _MainUiWidgetState with SingleTickerProviderStateMixin {
  Widget _buildControlPane(BuildContext context, int mode, int year,
      [List<FieldId> fields, List<TeamId> teams]) {
    return mode == UiMode.quick
        ? SizedBox(height: 1)
        : PagerSelectorWidget(
            year: year,
            enabled: !loading,
            onYearChanged: (value) {
              setState(() {
                gameList.clear();
                _year = value;
                loading = true;
              });
              //Future.delayed(_fetchDelay, () {
              fetchMonthList(_year, _month);
              //});
            },
            onFieldChanged: (value) {
              setState(() {
                _field = value;
              });
            },
            onFavTeamChanged: (value) {
              setState(() {
                _favTeam = value;
              });
            },
            teamList: teams,
            fieldList: fields,
          );
  }

  Widget _buildPagerWidget(BuildContext context, bool enabled) {
    List<Tab> titleList = new List();
    List<Widget> childList = new List();
    for (int i = 1; i <= CpblClient.monthList.length; i++) {
      titleList.add(new Tab(text: CpblClient.getMonthString(context, i)));
      childList.add(new ContentUiWidget(
        enabled: enabled,
        list: gameList[i],
        year: _year,
        month: _month,
        field: _field,
        favTeam: _favTeam,
      ));
    }
    return Column(
      children: <Widget>[
        Container(
          child: TabBar(
            controller: _tabController,
            tabs: titleList,
            labelColor: Theme.of(context).primaryColor,
            indicatorColor: Theme.of(context).primaryColor.withAlpha(224),
            //indicatorSize: TabBarIndicatorSize.tab,
            //indicatorWeight: 4,
            unselectedLabelColor: Colors.grey,
            isScrollable: true,
          ),
          color: Color.fromARGB(240, 240, 240, 240),
        ),
//        Container(
//          //child: SizedBox(height: 1),
//          constraints: BoxConstraints.expand(height: 1),
//          color: Theme.of(context).primaryColor.withAlpha(64),
//        ),
        Expanded(
          child: TabBarView(
            controller: _tabController,
            physics: NeverScrollableScrollPhysics(),
            children: childList,
          ),
        ),
      ],
    );
  }

  Widget _buildContentWidget(BuildContext context, int mode, List<Game> data, bool isLoading,
      TeamId teamId, FieldId field) {
    if (mode == UiMode.quick) {
      return ContentUiWidget(
        loading: isLoading,
        mode: mode,
        list: data,
        onHighlightPaneClosed: () {
          //pullToRefresh(2018, 10);
          setState(() {
            _mode = UiMode.list;
            //loading = true;
          });
          fetchMonthList(_year, _month);
        },
      );
    }
    if (isLoading) {
      //pager loading
      return Stack(
        alignment: Alignment.center,
        children: <Widget>[
          _buildPagerWidget(context, false),
          Positioned(child: CircularProgressIndicator(), top: 240),
        ],
      );
    }
    return _buildPagerWidget(context, true);
  }

  TabController _tabController;

  void onTabClickListener() {
    if (_tabController.indexIsChanging) {
      print('change tab selected: ${_tabController.index}');
      setState(() {
        loading = true;
        _month = _tabController.index + 1;
      });
      fetchMonthList(_year, _tabController.index + 1);
    }
  }

  Map<int, List<Game>> gameList = new Map();

  fetchMonthList(int year, int month, {bool forceUpdate = false}) async {
    print('pager: fetch $year/$month');
    setState(() {
      loading = true;
    });
    _startTimeout(_timeoutRefresh); //start fetch month list
    List<Game> list = new List();
    if (_client.isWarmUpMonth(year, month)) {
      list.addAll(await _client.fetchList(year, month, GameType.type_07, !forceUpdate));
      sleep(_fetchDelay); //warm up
    }
    list.addAll(await _client.fetchList(year, month, GameType.type_01, !forceUpdate));
    if (_client.isChallengeMonth(year, month)) {
      list.addAll(await _client.fetchList(year, month, GameType.type_05, !forceUpdate));
      sleep(_fetchDelay); //challenge
    }
    if (_client.isChampionMonth(year, month)) {
      list.addAll(await _client.fetchList(year, month, GameType.type_03, !forceUpdate));
      sleep(_fetchDelay); //championship
    }
    if (_client.isAllStarMonth(year, month)) {
      list.addAll(await _client.fetchList(year, month, GameType.type_02, !forceUpdate));
    }
    _clearTimeout(); //fetch month list complete
    print('fetch complete $month ${list.length}');
    list.sort((g1, g2) => g1.timeInMillis() == g2.timeInMillis()
        ? g1.id - g2.id
        : g1.timeInMillis() - g2.timeInMillis());
    List<FieldId> fields = [FieldId.f00];
    List<TeamId> teams = [TeamId.fav_all];
    list?.forEach((g) {
      if (!fields.contains(g.fieldId)) fields.add(g.fieldId);
      if (!teams.contains(g.home.id)) teams.add(g.home.id);
      if (!teams.contains(g.away.id)) teams.add(g.away.id);
    });
    fields.sort((a, b) => a.index - b.index);
    teams.sort((a, b) => a.index - b.index);
    setState(() {
      _fieldList = fields;
      _teamList = teams;
      gameList[month] = list;
      this.list = list;
      this.loading = false;
    });
  }

  @override
  void initState() {
    super.initState();
    _tabController = TabController(vsync: this, length: CpblClient.monthList.length);
    _tabController.addListener(onTabClickListener);
  }

  @override
  void startFetchGames() {
    print('init from $_year/$_month');
    if (_client.isHighlightEnabled()) {
      fetchHighlight(_year, _month, debug: widget.debug); //pager init
    } else {
      _tabController.animateTo(_month - 1); //pager init
    }
  }

  @override
  void refreshGameList() {
    if (loading) {
      print('still loading game list');
    } else if (_mode == UiMode.list) {
      fetchMonthList(_year, _month, forceUpdate: true); //pager refresh button
    } else {
      fetchHighlight(_year, _month, forceUpdate: true); //pager refresh button
    }
  }

  @override
  void dispose() {
    _tabController.removeListener(onTabClickListener);
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Scaffold(
        backgroundColor: Theme.of(context).primaryColor,
        appBar: AppBar(
          title: Text(
            Lang.of(context).trans('app_name'),
            //style: TextStyle(color: Colors.white),
          ),
          actions: buildOptionMenu(context, loading, _year),
          //leading: new Container(),
          automaticallyImplyLeading: false,
          elevation: 0.0,
        ),
        body: Column(
          children: <Widget>[
            _buildControlPane(context, _mode, _year, _fieldList, _teamList),
            Expanded(
              child: Container(
                child: _buildContentWidget(context, _mode, list, loading, _favTeam, _field),
                color: Colors.white,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
