package dolphin.android.apps.CpblCalendar.provider;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dolphin.android.apps.CpblCalendar.R;

@Keep
public class Game implements Parcelable {
    public int Id = 0;
    public String Kind = null;

    public Team HomeTeam = null;
    public int HomeScore = 0;

    public Team AwayTeam = null;
    public int AwayScore = 0;

    public String Field = null;
    public String FieldId;

    public Calendar StartTime;
    public boolean IsFinal = false;
    public String Url = null;//link to final website

    public String Channel = null;

    public boolean IsDelay = false;
    public String DelayMessage = null;

    public int Source = SOURCE_CPBL;

    public final static int SOURCE_CPBL = 0;
    public final static int SOURCE_ZXC22 = 1;
    public final static int SOURCE_CPBL_2013 = 2;

    public int People = 0;

    public boolean IsLive = false;//[181]++
    public String LiveMessage = null;//[181]++

    public Game() {

    }

    public Game(int id) {
        Id = id;
        StartTime = Calendar.getInstance();
    }

    protected Game(Parcel in) {
        Id = in.readInt();
        Kind = in.readString();

        HomeTeam = new Team(in.readInt(), in.readString(), in.readString(), in.readInt());
        HomeScore = in.readInt();
        AwayTeam = new Team(in.readInt(), in.readString(), in.readString(), in.readInt());
        AwayScore = in.readInt();

        Field = in.readString();
        FieldId = in.readString();

        StartTime = Calendar.getInstance();
        StartTime.setTimeInMillis(in.readLong());

        IsFinal = in.readByte() != 0;
        Url = in.readString();
        Channel = in.readString();
        IsDelay = in.readByte() != 0;
        DelayMessage = in.readString();
        Source = in.readInt();
        People = in.readInt();
        IsLive = in.readByte() != 0;
        LiveMessage = in.readString();
    }

    public static final Creator<Game> CREATOR = new Creator<Game>() {
        @Override
        public Game createFromParcel(Parcel in) {
            return new Game(in);
        }

        @Override
        public Game[] newArray(int size) {
            return new Game[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        int year = StartTime.get(Calendar.YEAR);
        parcel.writeInt(Id);
        parcel.writeString(Kind);

        //Home
        parcel.writeInt(HomeTeam.getId());
        parcel.writeString(HomeTeam.getName());
        parcel.writeString(HomeTeam.getShortName());
        parcel.writeInt(HomeTeam.getLogo(year));
        parcel.writeInt(HomeScore);

        //Away
        parcel.writeInt(AwayTeam.getId());
        parcel.writeString(AwayTeam.getName());
        parcel.writeString(AwayTeam.getShortName());
        parcel.writeInt(AwayTeam.getLogo(year));
        parcel.writeInt(AwayScore);

        parcel.writeString(Field);
        parcel.writeString(FieldId);

        parcel.writeLong(StartTime.getTimeInMillis());//time in millis

        parcel.writeByte((byte) (IsFinal ? 1 : 0));
        parcel.writeString(Url);
        parcel.writeString(Channel);
        parcel.writeByte((byte) (IsDelay ? 1 : 0));
        parcel.writeString(DelayMessage);
        parcel.writeInt(Source);
        parcel.writeInt(People);
        parcel.writeByte((byte) (IsLive ? 1 : 0));
        parcel.writeString(LiveMessage);
    }

    @Override
    public String toString() {
        //return super.toString();
        String delayMsg = DelayMessage != null ? DelayMessage : "";
        if (AwayTeam == null || HomeTeam == null) {//delay game list
            return String.format(Locale.US, "[%03d] %s %s", Id, getDisplayDate(), delayMsg);
        }
        if (IsFinal) {
            return String.format(Locale.US, "[%03d] %d vs %d [%d:%d], %s %s", Id, AwayTeam.getId(),
                    HomeTeam.getId(), AwayScore, HomeScore, getDisplayDate(), delayMsg);
        }
        return String.format(Locale.US, "[%03d] %d vs %d, %s %s", Id, AwayTeam.getId(),
                HomeTeam.getId(), getDisplayDate(), delayMsg);
    }

    /**
     * convert from Game object to string
     *
     * @param game game
     * @return string data
     */
    public static String toPrefString(Game game) {
        return String.format(Locale.US, "%d;%d;%d;%s;%s;%d;%s", game.Id,
                game.AwayTeam.getId(), game.HomeTeam.getId(),
                game.Field, game.Channel, game.StartTime.getTimeInMillis(), game.Kind);
    }


    /**
     * Format Calendar as proper time display string
     *
     * @param calendar Calendar
     * @return time display string
     */
    @SuppressWarnings("WeakerAccess")
    public static String getDisplayDate(Calendar calendar) {
        return String.format(Locale.US, "%04d/%02d/%02d %02d:%02d", calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }

    /**
     * Format StartTime as proper time display string
     *
     * @return time display string
     */
    public String getDisplayDate() {
        return getDisplayDate(StartTime);
    }

    /**
     * convert from string to Game object
     *
     * @param context Context
     * @param str     to json str
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static Game fromPrefString(Context context, String str) {
        String[] gInfo = str.split(";");
        //Log.d("dolphin", "size: " + gInfo.length);
        //for (String s : gInfo)
        //    Log.d("dolphin", " " + s);
        Game game = new Game();
        game.Id = Integer.decode(gInfo[0]);
        game.AwayTeam = new Team(context, Integer.decode(gInfo[1]));
        game.HomeTeam = new Team(context, Integer.decode(gInfo[2]));
        game.Field = gInfo[3];
        game.Channel = (gInfo[4].equals("null") || gInfo[4].trim().isEmpty()) ? null : gInfo[4];
        game.StartTime = Calendar.getInstance();
        game.StartTime.setTimeInMillis(Long.decode(gInfo[5]));
        game.Kind = gInfo.length < 7 ? "01" : gInfo[6];//[122]dolphin++ add Kind
        return game;
    }

    //http://google-gson.googlecode.com/svn/trunk/gson/docs/javadocs/com/google/gson/TypeAdapter.html
    private static class GameTypeAdapter extends TypeAdapter<Game> {
        private final Context mContext;

        GameTypeAdapter(Context context) {
            super();
            mContext = context;
        }

        @Override
        public void write(JsonWriter jsonWriter, Game game) throws IOException {
            if (game == null) {
                jsonWriter.nullValue();
                return;
            }

            writeOneGame(jsonWriter, game);
        }

        static void writeOneGame(JsonWriter jsonWriter, Game game) throws IOException {
            jsonWriter.beginObject();

            jsonWriter.name("Id").value(game.Id);
            jsonWriter.name("Kind").value(game.Kind);
            jsonWriter.name("StartTime").value(game.StartTime.getTimeInMillis());

            jsonWriter.name("AwayTeamId").value(game.AwayTeam.getId());
            if (game.AwayTeam.getId() == Team.ID_UNKNOWN) {
                jsonWriter.name("AwayTeamName").value(game.AwayTeam.getName());
            }
            jsonWriter.name("HomeTeamId").value(game.HomeTeam.getId());
            if (game.HomeTeam.getId() == Team.ID_UNKNOWN) {
                jsonWriter.name("HomeTeamName").value(game.HomeTeam.getName());
            }

            jsonWriter.name("Source").value(game.Source);
            if (game.Source == Game.SOURCE_ZXC22) {
                jsonWriter.name("People").value(game.People);
            }

            jsonWriter.name("IsFinal").value(game.IsFinal);
            if (game.IsFinal) {
                jsonWriter.name("AwayScore").value(game.AwayScore);
                jsonWriter.name("HomeScore").value(game.HomeScore);
            }

            if (game.Field != null) {
                jsonWriter.name("Field").value(game.Field);
            }
            if (game.Channel != null) {
                jsonWriter.name("Channel").value(game.Channel);
            }
            if (game.Url != null) {
                jsonWriter.name("Url").value(game.Url);
            }

            jsonWriter.name("IsDelay").value(game.IsDelay);
            if (game.IsDelay) {
                jsonWriter.name("DelayMessage").value(game.DelayMessage);
            }
            jsonWriter.endObject();
        }

        @Override
        public Game read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return readOneGame(mContext, jsonReader);
        }

        static Game readOneGame(Context context, JsonReader jsonReader) throws IOException {
            Game game = new Game();
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                switch (name) {
                    case "Id":
                        game.Id = jsonReader.nextInt();
                        break;
                    case "Kind":
                        game.Kind = jsonReader.nextString();
                        break;
                    case "StartTime":
                        game.StartTime = CpblCalendarHelper.getNowTime();
                        game.StartTime.setTimeInMillis(jsonReader.nextLong());
                        break;
                    case "Source":
                        game.Source = jsonReader.nextInt();
                        break;
                    case "Field":
                        game.Field = jsonReader.nextString();
                        break;
                    case "AwayTeamId":
                        game.AwayTeam = new Team(context, jsonReader.nextInt());
                        break;
                    case "HomeTeamId":
                        game.HomeTeam = new Team(context, jsonReader.nextInt());
                        break;
                    case "IsFinal":
                        game.IsFinal = jsonReader.nextBoolean();
                        break;
                    case "AwayScore":
                        game.AwayScore = jsonReader.nextInt();
                        break;
                    case "HomeScore":
                        game.HomeScore = jsonReader.nextInt();
                        break;
                    case "People":
                        game.People = jsonReader.nextInt();
                        break;
                    case "Url":
                        game.Url = jsonReader.nextString();
                        break;
                    case "IsDelay":
                        game.IsDelay = jsonReader.nextBoolean();
                        break;
                    case "Channel":
                        game.Channel = jsonReader.nextString();
                        break;
                    case "DelayMessage":
                        game.DelayMessage = jsonReader.nextString();
                        break;
                    case "HomeTeamName": //this may override the HomeTeam
                        game.HomeTeam = new Team(context, jsonReader.nextString(), 2014);
                        break;
                    case "AwayTeamName": //this may override the AwayTeam
                        game.AwayTeam = new Team(context, jsonReader.nextString(), 2014);
                        break;
                }
            }
            jsonReader.endObject();
            return game;
        }
    }

    public static String toJson(Context context, Game game) {
        Type type = new TypeToken<Game>() {
        }.getType();
        Gson gson = new GsonBuilder().registerTypeAdapter(type, new GameTypeAdapter(context)).create();
        //Log.d(TAG, gson.toJson(game));
        return gson.toJson(game);
    }

    public static Game fromJson(Context context, String json) {
        Type type = new TypeToken<Game>() {
        }.getType();
        Gson gson = new GsonBuilder().registerTypeAdapter(type, new GameTypeAdapter(context)).create();
        return gson.fromJson(json, type);
    }

    private static class GameListTypeAdapter extends TypeAdapter<ArrayList<Game>> {
        private final Context mContext;

        GameListTypeAdapter(Context context) {
            super();
            mContext = context;
        }

        @Override
        public void write(JsonWriter jsonWriter, ArrayList<Game> games) throws IOException {
            if (games == null) {
                jsonWriter.nullValue();
                return;
            }

            jsonWriter.beginArray();
            for (Game game : games) {
                GameTypeAdapter.writeOneGame(jsonWriter, game);
            }
            jsonWriter.endArray();
        }

        @Override
        public ArrayList<Game> read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }

            ArrayList<Game> list = new ArrayList<Game>();
            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                list.add(GameTypeAdapter.readOneGame(mContext, jsonReader));
            }
            jsonReader.endArray();
            return list;
        }
    }

    static String listToJson(Context context, ArrayList<Game> games) {
        //Type type = new TypeToken<List<Game>>() {
        //}.getType();
        Gson gson = new GsonBuilder().registerTypeAdapter(ArrayList.class,
                new GameListTypeAdapter(context)).create();
        //Log.d(TAG, gson.toJson(games));
        return gson.toJson(games);
    }

    static ArrayList<Game> listFromJson(Context context, String json) {
        Type type = new TypeToken<List<Game>>() {
        }.getType();
        Gson gson = new GsonBuilder().registerTypeAdapter(type,
                new GameListTypeAdapter(context)).create();
        return gson.fromJson(json, type);
    }

    public boolean canOpenUrl() {
        if (Url == null || Url.isEmpty()) {
            return false;
        }
        Calendar now = CpblCalendarHelper.getNowTime();
        boolean enabled = Url.contains("box.html") && StartTime.before(now);//past
        enabled |= StartTime.after(now) && Url.contains("starters.html");//future
        enabled |= Url.contains("play_by_play.html");//live
        return enabled;
    }

    public String getFieldId(Context context) {
        return getFieldId(context, this);
    }

    public static String getFieldId(Context context, Game game) {
        if (game.Field.equals(context.getString(R.string.cpbl_game_field_name_F19))) {
            game.FieldId = "F19";
        } else if (game.Field.equals(context.getString(R.string.cpbl_game_field_name_F23))) {
            game.FieldId = "F23";
        } else {//check list
            String[] fields = context.getResources().getStringArray(R.array.cpbl_game_field_name);
            String[] fieldIds = context.getResources().getStringArray(R.array.cpbl_game_field_id);
            for (int i = 0; i < fieldIds.length; i++) {
                if (game.Field.equals(fields[i])) {
                    game.FieldId = fieldIds[i];
                    break;
                }
            }
        }
        return game.FieldId;
    }
}
