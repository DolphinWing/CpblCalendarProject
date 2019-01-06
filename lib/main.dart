import 'dart:async';

import 'package:firebase_remote_config/firebase_remote_config.dart';
import 'package:flutter/material.dart';
import 'package:flutter\_localizations/flutter\_localizations.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';

import 'content.dart';
import 'cpbl.dart';
import 'drawer.dart';
import 'lang.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      //title: Lang.of(context).trans('app_name'),
      theme: ThemeData(
        primarySwatch: Colors.green,
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

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Container(
        color: Colors.white,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
//          Image(
//            image: AssetImage('resources/drawable/splash.png'),
//          ),
            SizedBox(
              height: 36,
            ),
            CircularProgressIndicator(),
          ],
        ),
      ),
    );
  }

  @override
  void initState() {
    super.initState();
    prepare();
  }

  prepare() async {
    var _duration = new Duration(microseconds: 200);
    return new Timer(_duration, navigationPage);
    //await Lang.of(context).load();
    //_navigationPage();
  }

  void navigationPage() async {
    configs = await RemoteConfig.instance;
    final defaults = <String, dynamic>{'welcome': 'default welcome'};
    await configs.setConfigSettings(new RemoteConfigSettings(debugMode: true));
    await configs.setDefaults(defaults);
    await configs.fetch(expiration: const Duration(hours: 5));
    await configs.activateFetched();
    print('welcome message: ${configs.getString('welcome')}');
    print('  override: ${configs.getBool('override_start_enabled')}');
    final prefs = await SharedPreferences.getInstance();
    final CpblClient client = new CpblClient(context, configs, prefs);
    await client.init();

    //load large json file in localizationsDelegates will cause black screen
    await Lang.of(context).load(); //late load
    //Navigator.of(context).pushReplacementNamed('/calendar');
    Navigator.of(context)
        .pushReplacement(MaterialPageRoute(builder: (context) => MainUiWidget(client: client)));
  }
}

class MainUiWidget extends StatefulWidget {
  final bool debug;
  final CpblClient client;

  MainUiWidget({this.debug = false, this.client});

  @override
  State<StatefulWidget> createState() => _MainUiWidgetState();
}

class _MainUiWidgetState extends State<MainUiWidget> {
  //https://www.reddit.com/r/FlutterDev/comments/7yma7y/how_do_you_open_a_drawer_in_a_scaffold_using_code/duhllqz
  GlobalKey<ScaffoldState> _scaffoldKey = new GlobalKey();

  Timer _timer;
  CpblClient _client;

  bool loading = false;
  List<Game> list;
  bool showFab = false;
  int _year;
  int _month;
  GameType _type;
  int _mode = UiMode.list;

  @override
  void initState() {
    super.initState();

    int year = 2018;
    int month = 10;
    setState(() {
      _year = year;
      _month = month;
    });

    /// Flutter get context in initState method
    /// https://stackoverflow.com/a/49458289/2673859
    new Future.delayed(Duration.zero, () async {
      if (widget.client != null) {
        _client = widget.client;
        //already init before
        if (_client.isHighlightEnabled()) {
          fetchHighlight(year, month, debug: widget.debug);
        } else {
          pullToRefresh(year, month, debug: widget.debug);
        }
        setState(() {
          showFab = !_client.isHighlightEnabled();
        });
      } else {
        //loaded in splash
        final configs = await RemoteConfig.instance;

        if (configs.getBool('override_start_enabled')) {
          year = configs.getInt('override_start_year');
          month = configs.getInt('override_start_month');
          print('override: year=$year month=$month');
        }

        final prefs = await SharedPreferences.getInstance();
        _client = new CpblClient(context, configs, prefs);

        bool showHighlight = _client.isHighlightEnabled();
        setState(() {
          showFab = !showHighlight;
        });

        //var now = DateTime.now();
        _client.init().then((value) {
          if (showHighlight) {
            fetchHighlight(year, month, debug: widget.debug);
          } else {
            pullToRefresh(year, month, debug: widget.debug);
          }
        });
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void pullToRefresh(int year, int month, {GameType type = GameType.type_01, bool debug = false}) {
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
      _client.fetchList(year, month, type).then((gameList) {
        setState(() {
          list = gameList;
          loading = false;
        });
      });
    }
  }

  Future<List<Game>> _fetchList(int year, int month, GameType type, [DateTime time]) async {
    List<Game> list = new List();
    var list1 = await _client.fetchList(year, month, type);
    if (list1.isNotEmpty) {
      if (list1.last.before(time)) {
        print('last game is before now');
        list.addAll(list1);
        var list2 = await _client.fetchList(year, month + 1, type);
        list.addAll(list2);
      } else if (list1.first.after(time)) {
        print('first game is not yet coming');
        list.addAll(await _client.fetchList(year, month - 1, type));
        list.addAll(list1);
      } else {
        print('add ${list1.length} to normal list');
        list.addAll(list1);
      }
    }
    return list;
  }

  void fetchHighlight(int year, int month, {bool debug = false}) async {
    setState(() {
      loading = true;
      _mode = UiMode.quick;
      showFab = false;
    });

    List<Game> gameList = new List();
    //load version update
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
      var time = DateTime(2018, 10, 10);
      List<Game> list = new List();
      if (_client.isWarmUpMonth(year, month)) {
        list.addAll(await _client.fetchList(year, month, GameType.type_07)); //warm
      }
      list.addAll(await _fetchList(year, month, GameType.type_01, time));
      if (_client.isChallengeMonth(year, month)) {
        list.addAll(await _fetchList(year, month, GameType.type_02, time)); //challenge
      }
      if (_client.isChampionMonth(year, month)) {
        list.addAll(await _fetchList(year, month, GameType.type_03, time)); //champion
      }
      if (_client.isAllStarMonth(year, month)) {
        list.addAll(await _client.fetchList(year, month, GameType.type_05)); // all star
      }
      gameList.addAll(_client.getHighlightGameList(list, time));
    }
    //show more button
    if (gameList.isNotEmpty) {
      gameList.add(Game.more());
      setState(() {
        list = gameList;
        loading = false;
      });
    } else {
      setState(() {
        list = gameList;
        loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Scaffold(
        key: _scaffoldKey,
        appBar: AppBar(
          title: Text(Lang.of(context).trans('app_name')),
          actions: <Widget>[
            IconButton(
              icon: Icon(Icons.refresh),
              onPressed: () {
                if (_mode == UiMode.list) {
                  pullToRefresh(_year, _month, type: _type);
                } else {
                  fetchHighlight(_year, _month);
                }
              },
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
                    PopupMenuItem(
                      value: 2,
                      child: Text('show highlight'),
                    ),
                  ],
              onSelected: (action) async {
                //print('selected option $action');
                switch (action) {
                  case 0:
                    if (await canLaunch(CpblClient.homeUrl)) {
                      await launch(CpblClient.homeUrl);
                    } else {
                      throw 'Could not launch ${CpblClient.homeUrl}';
                    }
                    break;
                  case 1:
                    //Navigator.of(context).pushNamed('/settings');
                    Navigator.of(context).push(
                        MaterialPageRoute(builder: (context) => SettingsPane(client: _client)));
                    break;
                  case 2:
                    fetchHighlight(_year, _month, debug: true);
                    break;
                }
              },
            ),
          ],
          //leading: new Container(),
          automaticallyImplyLeading: false,
          //elevation: _mode == UiMode.quick ? -1.0 : 4.0,
        ),
        endDrawer: DrawerPane(
          year: _year,
          month: _month,
          type: _type,
          onPressed: (action) {
            print('query ${action.year}/${action.month} ${action.field} ${action.type}');
            pullToRefresh(action.year, action.month, type: action.type);
          },
        ),
        body: ContentUiWidget(
          loading: loading,
          mode: _mode,
          list: list,
          onScrollEnd: (value) {
            setState(() {
              showFab = !value && _mode == UiMode.list;
            });
          },
          onHighlightPaneClosed: () {
            pullToRefresh(2018, 10);
          },
        ),
        floatingActionButton: showFab
            ? FloatingActionButton(
                child: Icon(
                  Icons.search,
                  //color: Theme.of(context).primaryColor,
                ),
                onPressed: () {
                  //print('search');
                  //Scaffold.of(context).openEndDrawer();
                  _scaffoldKey.currentState.openEndDrawer();
                },
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
  bool _enableViewPager = false;
  bool _enableHighlight = false;

  @override
  void initState() {
    super.initState();
    prepare();
  }

  void prepare() async {
    setState(() {
      _enableViewPager = widget.client?.isViewPagerEnabled() ?? false;
      _enableHighlight = widget.client?.isHighlightEnabled() ?? false;
    });
  }

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
            Divider(),
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
            Divider(),
          ],
        ),
      ),
    );
  }
}
