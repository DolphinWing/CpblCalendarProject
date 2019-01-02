import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter\_localizations/flutter\_localizations.dart';

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
      supportedLocales: [const Locale('en', 'US'), const Locale('zh', 'TW')],
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
        '/calendar': (context) => MainUiWidget(),
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
  @override
  State<StatefulWidget> createState() => MainUiWidgetState();
}

class MainUiWidgetState extends State<MainUiWidget> {
  //https://www.reddit.com/r/FlutterDev/comments/7yma7y/how_do_you_open_a_drawer_in_a_scaffold_using_code/duhllqz
  GlobalKey<ScaffoldState> _scaffoldKey = new GlobalKey();

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Scaffold(
        key: _scaffoldKey,
        endDrawer: Drawer(
          //constraints: BoxConstraints(minWidth: 240),
          child: Column(
            children: <Widget>[
              Text('drawer'),
              //Expanded(child: SizedBox()),
            ],
          ),
          //color: Colors.white,
        ),
        body: Text(Lang.of(context).trans('app_name')),
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
