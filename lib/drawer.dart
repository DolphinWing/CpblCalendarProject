import 'package:flutter/material.dart';

class Query {
  int year;
  int month;
  int field;
}

class DrawerPane extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => _DrawerPaneState();
}

class _DrawerPaneState extends State<DrawerPane> {
  int _year;
  int _month;

  @override
  void initState() {
    super.initState();
    var now = DateTime.now();
    setState(() {
      _year = now.year;
      _month = now.month;
    });
  }

  Widget buildYearWidget(BuildContext context, int year) {
    var now = DateTime.now();
    List<DropdownMenuItem<int>> list = new List();
    for (int i = now.year; i >= 1990; i--) {
      list.add(new DropdownMenuItem(
        child: Text('year $i'),
        value: i,
      ));
    }
    return DropdownButton<int>(
      value: year,
      items: list,
      onChanged: (value) {
        setState(() {
          _year = value;
        });
      },
      isExpanded: true,
    );
  }

  Widget buildMonthWidget(BuildContext context, int month) {
    return DropdownButton<int>(
      value: month,
      items: [
        DropdownMenuItem(
          value: DateTime.january,
          child: Text('${DateTime.january}'),
        ),
        DropdownMenuItem(
          value: DateTime.february,
          child: Text('${DateTime.february}'),
        ),
        DropdownMenuItem(
          value: DateTime.march,
          child: Text('${DateTime.march}'),
        ),
        DropdownMenuItem(
          value: DateTime.april,
          child: Text('${DateTime.april}'),
        ),
        DropdownMenuItem(
          value: DateTime.may,
          child: Text('${DateTime.may}'),
        ),
        DropdownMenuItem(
          value: DateTime.june,
          child: Text('${DateTime.june}'),
        ),
      ],
      onChanged: (value) {
        setState(() {
          _month = value;
        });
      },
      isExpanded: true,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Drawer(
      //constraints: BoxConstraints(minWidth: 240),
      child: Padding(
        child: Column(
          children: <Widget>[
            Expanded(
              child: ListView(
                children: <Widget>[
                  Text('year'),
                  buildYearWidget(context, _year),
                  Text('month'),
                  buildMonthWidget(context, _month),
                  Text('field'),
                  Text('teams'),
                ],
              ),
            ),
            //Expanded(child: SizedBox()),
            Container(
              constraints: BoxConstraints.expand(height: 48),
              child: RaisedButton(
                child: Text('query'),
                onPressed: () {
                  Navigator.of(context).pop();
                },
                color: Theme.of(context).primaryColor,
              ),
              margin: EdgeInsets.only(bottom: 8),
            ),
          ],
        ),
        padding: EdgeInsets.only(top: 8, bottom: 8, left: 16, right: 16),
      ),
      //color: Colors.white,
    );
  }
}
