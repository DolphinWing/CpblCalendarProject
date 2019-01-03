import 'package:flutter/material.dart';

import 'cpbl.dart';

class UiMode {
  static const int list = 0;
  static const int quick = 1;
}

class ContentUiWidget extends StatefulWidget {
  final bool loading;
  final List<Game> list;
  final int mode;

  ContentUiWidget({this.loading = false, List<Game> list, this.mode = UiMode.list})
      : this.list = list ?? new List();

  @override
  State<StatefulWidget> createState() => _ContentUiWidgetState();
}

class _ContentUiWidgetState extends State<ContentUiWidget> {
  Widget buildContent(BuildContext context, List<Game> list, int mode) {
    List<Widget> widgetList = new List();
    list?.forEach((game) {
      widgetList.add(GameCardWidget(game));
    });
    return ListView(
      children: widgetList,
    );
  }

  @override
  Widget build(BuildContext context) {
    return widget.loading
        ? Stack(
            alignment: Alignment.center,
            children: <Widget>[
              buildContent(context, widget.list, widget.mode),
              Positioned(child: CircularProgressIndicator(), top: 200),
            ],
          )
        : buildContent(context, widget.list, widget.mode);
  }
}

class TeamRowWidget extends StatelessWidget {
  final Team team;

  TeamRowWidget(this.team);

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: <Widget>[
        Padding(
          child: Icon(
            Icons.donut_small,
            color: team.color,
          ),
          padding: EdgeInsets.all(8),
        ),
        Expanded(
          child: Text(
            team.getDisplayName(context),
            style: Theme.of(context).textTheme.title,
          ),
        ),
        Padding(
          child: Text(
            '${team.score}',
            style: Theme.of(context).textTheme.title,
          ),
          padding: EdgeInsets.all(4),
        ),
      ],
    );
  }
}

class GameCardWidget extends StatelessWidget {
  final Game game;

  GameCardWidget(this.game);

  @override
  Widget build(BuildContext context) {
    return new Container(
      padding: EdgeInsets.only(left: 16, right: 16, top: 8, bottom: 8),
      margin: EdgeInsets.only(top: 4, bottom: 4, left: 8, right: 8),
      child: Column(
        children: <Widget>[
          //Divider(),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: <Widget>[
              Text(
                game.getDisplayTime(),
                style: Theme.of(context).textTheme.subhead,
              ),
              Expanded(child: SizedBox()),
              Text(
                '${game.getGameType(context)} ${game.id}',
                style: Theme.of(context).textTheme.subtitle,
              ),
            ],
          ),
          TeamRowWidget(game.away),
          TeamRowWidget(game.home),
          Row(
            children: <Widget>[
              Text(game.getFieldName(context)),
            ],
          ),
          game.extra != null ? Text('${game.extra}') : SizedBox(height: 1),
          //Divider(),
        ],
      ),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.all(Radius.circular(8)),
        //color: Colors.grey.withAlpha(32),
        border: Border.all(
          style: BorderStyle.solid,
          color: Colors.grey.withAlpha(64),
        ),
      ),
    );
  }
}
