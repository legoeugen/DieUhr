package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.StrictMode;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private TextView WochentagText;
    private TextView ZeitText;
    private TextView CurrentLessonText;
    private TextView CurrentHourText;
    private TextView CurrentRoomText;
    private TextView LessonStartText;
    private TextView LessonEndText;
    private TextView NextLessonText;
    private TextView NextRoomText;
    private TextView NextTextText;
    private TextView NextHourText;

    private ProgressBar LessonProgressBar;

    private View NextLessonDivider;
    private View NextHourDivider;

    private int hour;
    private int min;
    private int sek;

    final static String[] DAYS = {
            "Sonntag","Montag","Dinstag","Mittwoch","Donerstag","Freitag","Samstag"
    };

    //private int day;

    String currentLessonplan;

    Thread t1 = new Thread(() -> StartClock());

    public List<Lesson> dayLessons = new ArrayList<>();

    private int currentHour;
    private String StartTime;
    private String EndTime;

    private String TimeToDownloadPlan = "23:49:00";

    private Calendar mCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCalendar = Calendar.getInstance();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build());

        setContentView(R.layout.activity_main);

        WochentagText = findViewById(R.id.Wochentag);
        ZeitText = findViewById(R.id.Zeit);
        CurrentLessonText = findViewById(R.id.Lesson);
        CurrentHourText = findViewById(R.id.current_Hour);
        CurrentRoomText = findViewById(R.id.current_Room);
        LessonStartText = findViewById(R.id.LessonStart);
        LessonEndText = findViewById(R.id.LessonEnd);
        NextLessonText = findViewById(R.id.NextLesson);
        NextRoomText = findViewById(R.id.NextRoom);
        LessonProgressBar = findViewById(R.id.progressBar);
        NextLessonDivider = findViewById(R.id.NextLessonDivider);
        NextHourDivider = findViewById(R.id.NextHourDivider);
        NextTextText = findViewById(R.id.NextText);
        NextHourText = findViewById(R.id.next_Hour);

        GetDate();

        //GetCurrentLessonTest(7,35,true);
        AutoDownloadTimer();
        DownloadPlan();

        t1.start();
    }

    private void GetDate(){
        int day = mCalendar.get(Calendar.DAY_OF_WEEK);

        WochentagText.setText(DAYS[day - 1]);
        SetCurrentLessonplan(DAYS[day - 1]);
    }

    private void SetCurrentLessonplan(String day){
        try {

            if (day == "Samstag" || day == "Sonntag")
                currentLessonplan = "Montag";
            else
                currentLessonplan = day;

            InputStream is = getAssets().open(currentLessonplan + ".txt");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                while (reader.ready()) {
                    String line = reader.readLine();

                    String[] result = line.split(",");

                    Lesson newLesson = new Lesson(result[0],result[1]);

                    dayLessons.add(newLesson);
                }
            }catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (Exception e){
            e.printStackTrace();
        }


    }

    private void StartClock(){
        try {
            // create a clock
            ZoneId zid = ZoneId.of("Europe/Paris");

            while (true) {
                LocalDateTime lt
                        = LocalDateTime.now(zid);

                hour = lt.getHour();
                min = lt.getMinute();
                sek = lt.getSecond();

                ZeitText.setText(hour + ":" + min + ":" + sek);

                if (currentLessonplan != "Mittwoch") { //TODO activate this but not every second
                    GetCurrentLessonTest(hour, min, false);
                }else {
                    GetCurrentLessonTest(hour,min,true);
                }

                try {
                    LessonProgressBar.setMax(Integer.parseInt(EndTime));
                    LessonProgressBar.setMin(Integer.parseInt(StartTime));
                    String timeForProgressBar;
                    if (min >= 10) {
                        timeForProgressBar = hour + "" + min;
                    } else {
                        timeForProgressBar = hour + "0" + min;
                    }
                    LessonProgressBar.setProgress(Integer.parseInt(timeForProgressBar));
                }catch (Exception e){
                    System.out.println("Keine start und/oder end zeit warscheinlich gegeben.");
                }

                try {
                    t1.sleep(500);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void GetCurrentLessonTest(int hour, int minute,boolean wednesday){
        try{
            InputStream is;
            if (!wednesday) {
                is = getAssets().open("HourData.txt");
            }
            else {
                is = getAssets().open("HourDataWednesday.txt");
            }
            if (is == null){
                System.out.println("NULL");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                while (reader.ready()) {
                    String line = reader.readLine();
                    String[] result = line.split("-"); //z.b. 07:30-08:15 => 07:30 && 08:15
                    String[] result2 = result[0].split(":"); //z.b. 07:30 => 07 && 30
                    String[] result3 = result[1].split(":"); //z.b. 08:15 => 08 && 15

                    String string1 = result[0] + ":00";
                    Date time1 = new SimpleDateFormat("HH:mm:ss").parse(string1);
                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.setTime(time1);
                    calendar1.add(Calendar.DATE, 1);


                    String string2 = result[1] + ":00";
                    Date time2 = new SimpleDateFormat("HH:mm:ss").parse(string2);
                    Calendar calendar2 = Calendar.getInstance();
                    calendar2.setTime(time2);
                    calendar2.add(Calendar.DATE, 1);

                    String someRandomTime = hour + ":" + minute + ":00";
                    Date d = new SimpleDateFormat("HH:mm:ss").parse(someRandomTime);
                    Calendar calendar3 = Calendar.getInstance();
                    calendar3.setTime(d);
                    calendar3.add(Calendar.DATE, 1);

                    Date x = calendar3.getTime();
                    if (x.after(calendar1.getTime()) && x.before(calendar2.getTime())) {
                        System.out.println(Integer.parseInt(result2[0]));
                        StartTime = result2[0].replaceFirst("^0+(?!$)", "") + "" + result2[1];
                        EndTime = result3[0].replaceFirst("^0+(?!$)", "") + "" + result3[1];

                        switch (Integer.parseInt(result2[0])){
                            case 7:
                                currentHour = 1;
                                break;
                            case 8:
                                currentHour = 2;
                                break;
                            case 9:
                                currentHour = 3;
                                break;
                            case 10:
                                currentHour = 4;
                                break;
                            case 11:
                                currentHour = 5;
                                break;
                            case 12:
                                currentHour = 6;
                                break;
                            case 13:
                                currentHour = 7;
                                break;
                            case 14:
                                currentHour = 8;
                                break;
                            case 15:
                                currentHour = 9;
                                break;
                        }
                        CurrentHourText.setText(Integer.toString(currentHour));
                        NextHourText.setText(Integer.toString(currentHour + 1));

                        System.out.println("get current lesson and stuff with current hour: " + currentHour);

                        Lesson currentLesson;
                        Lesson nextLesson;

                        try {
                            currentLesson = dayLessons.get(currentHour - 1);
                            System.out.println("current Lesson: " + currentLesson.Lesson);
                            NextLessonDivider.setVisibility(View.VISIBLE);
                            NextTextText.setVisibility(View.VISIBLE);

                        }catch (Exception e){
                            System.out.println("keine lessons mehr");
                            UpdateUiEndOfSchool();
                            NextLessonDivider.setVisibility(View.INVISIBLE);
                            NextTextText.setVisibility(View.INVISIBLE);
                            return;
                        }

                        try{
                            nextLesson = dayLessons.get(currentHour);
                            NextLessonDivider.setVisibility(View.VISIBLE);
                            NextTextText.setVisibility(View.VISIBLE);
                        } catch (Exception e){
                            System.out.println("Keine nächste/n stunde/n");
                            nextLesson = new Lesson("","");
                            NextLessonDivider.setVisibility(View.INVISIBLE);
                            NextTextText.setVisibility(View.INVISIBLE);
                        }

                        UpdateUI(result[0], result[1], currentLesson.Lesson, currentLesson.Room, nextLesson.Lesson, nextLesson.Room);

                        break;
                    }
                    else {
                        UpdateUiPause();
                    }
                }
            }catch (FileNotFoundException e) {
                System.out.println("Null");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Null");
                e.printStackTrace();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void UpdateUiEndOfSchool(){
        System.out.println("End of school for today");
        CurrentLessonText.setText("End!");
        CurrentRoomText.setTextSize(50);
        CurrentRoomText.setText("End!");
        LessonStartText.setText("");
        LessonEndText.setText("");
        NextLessonText.setText("");
        NextRoomText.setText("");
        NextLessonDivider.setVisibility(View.INVISIBLE);
        NextHourDivider.setVisibility(View.INVISIBLE);
    }

    private void UpdateUI(String startTime,String endTime,String Lesson,String Room,String NextLesson,String NextRoom){
        System.out.println("Update UI with: " + startTime + "," + endTime + "," + Lesson + "," + Room + "," + NextLesson + "," + NextRoom);
        CurrentLessonText.setText(Lesson);
        CurrentRoomText.setText(Room);
        CurrentRoomText.setTextSize(50);
        LessonStartText.setText(startTime);
        LessonEndText.setText(endTime);
        NextLessonText.setText(NextLesson);
        NextRoomText.setText(NextRoom);
    }

    private void UpdateUiPause(){
        CurrentLessonText.setText("Pause");
        CurrentRoomText.setText("Pause");
        CurrentRoomText.setTextSize(45);
    }

    private void AutoDownloadTimer(){
        String[] result = TimeToDownloadPlan.split(":"); //23:10:00 => 23 && 10 && 00

        Timer timer = new Timer();
        Calendar date = Calendar.getInstance();
        date.set(
                Calendar.DAY_OF_WEEK,
                Calendar.SATURDAY
        );
        date.set(Calendar.HOUR, Integer.parseInt(result[0]));
        date.set(Calendar.MINUTE, Integer.parseInt(result[1]));
        date.set(Calendar.SECOND, Integer.parseInt(result[2]));
        date.set(Calendar.MILLISECOND, 0);
        //timer.schedule( //TODO make this shit work (error in this line)
        //        DownloadPlan(),
        //        date.getTime(),
        //        1000 * 60 * 60 * 24 * 7
        //);
    }

    private TimerTask DownloadPlan(){
        System.out.println("Download Plan!");
        //TODO download plan



        try {
            File planFile = new File("Plan.pdf");

            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try  {
                        downloadFileFromURL("https://www.lessing-oberschule-schkeuditz.de/.cm4all/uproc.php/0/Vertretungspl%C3%A4ne%202022/01.12.%20Do%20Sch%C3%BCler.pdf?cdp=a&_=184c8615e21",planFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();

        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public static void downloadFileFromURL(String urlString, File destination) {
        try {
            URL website = new URL(urlString);
            ReadableByteChannel rbc;
            rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(destination); //TODO: write permission
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void Extract_Table() { //ToDo pdf tabelle extrahiren
        // Load source PDF document
        String filePath = ""; // ToDo getAssets().open("Plan.pdf").toString();
        com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(filePath);
        com.aspose.pdf.TableAbsorber absorber = new com.aspose.pdf.TableAbsorber();

        // Scan pages
        for (com.aspose.pdf.Page page : pdfDocument.getPages()) {
            absorber.visit(page);
            for (com.aspose.pdf.AbsorbedTable table : absorber.getTableList()) {
                System.out.println("Table");
                // Iterate throught list of rows
                for (com.aspose.pdf.AbsorbedRow row : table.getRowList()) {
                    // Iterate throught list of cell
                    for (com.aspose.pdf.AbsorbedCell cell : row.getCellList()) {
                        for (com.aspose.pdf.TextFragment fragment : cell.getTextFragments()) {
                            StringBuilder sb = new StringBuilder();
                            for (com.aspose.pdf.TextSegment seg : fragment.getSegments())
                                sb.append(seg.getText());
                            System.out.print(sb.toString() + "|");
                        }
                    }
                    System.out.println();
                }
            }
        }
    }


    public class Lesson{
        String Lesson;
        String Room;

        public Lesson(String Lesson,String Room){
            this.Lesson = Lesson;
            this.Room = Room;
        }
    }
}

