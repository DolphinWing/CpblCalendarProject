import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter\_localizations/flutter\_localizations.dart';

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
        '/calendar': (context) => MainUiWidget(debug: true),
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
    //load large json file in localizationsDelegates will cause black screen
    await Lang.of(context).load(); //late load
    Navigator.of(context).pushReplacementNamed('/calendar');
  }
}

class MainUiWidget extends StatefulWidget {
  final bool debug;

  MainUiWidget({this.debug = false});

  @override
  State<StatefulWidget> createState() => _MainUiWidgetState();
}

class _MainUiWidgetState extends State<MainUiWidget> {
  //https://www.reddit.com/r/FlutterDev/comments/7yma7y/how_do_you_open_a_drawer_in_a_scaffold_using_code/duhllqz
  GlobalKey<ScaffoldState> _scaffoldKey = new GlobalKey();
  Timer _timer;
  CpblClient client;

  bool loading = false;
  List<Game> list;

  @override
  void initState() {
    super.initState();
    client = new CpblClient();
    //var now = DateTime.now();
    pullToRefresh(2018, 11, widget.debug);
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void pullToRefresh(int year, int month, [bool debug = false]) {
    setState(() {
      loading = true;
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
      client.fetchList(year, month).then((gameList) {
        setState(() {
          list = gameList;
          loading = false;
        });
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
            PopupMenuButton(
              itemBuilder: (context) => [
                    PopupMenuItem(
                      child: Text('xxx'),
                    ),
                    PopupMenuItem(
                      child: Text('xxx'),
                    ),
                  ],
            ),
          ],
          //leading: new Container(),
          automaticallyImplyLeading: false,
        ),
        endDrawer: DrawerPane(
          onPressed: (action) {
            print('query ${action.year}/${action.month} ${action.field}');
            //pullToRefresh(action.year, action.month);
          },
        ),
        body: ContentUiWidget(
          loading: loading,
          list: list,
        ),
        floatingActionButton: FloatingActionButton(
          child: Icon(
            Icons.search,
            //color: Theme.of(context).primaryColor,
          ),
          onPressed: () {
            //print('search');
            //Scaffold.of(context).openEndDrawer();
            _scaffoldKey.currentState.openEndDrawer();
          },
        ),
      ),
    );
  }
}
