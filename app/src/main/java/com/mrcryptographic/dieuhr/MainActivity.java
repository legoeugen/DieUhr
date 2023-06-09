package com.mrcryptographic.dieuhr;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mrcryptographic.dieuhr.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends Activity
{
    //References
    Map<String, Lesson> dayLessons = new HashMap<>();

    private Lesson currentLesson;
    private int currentHour;

    String lessonPlanForTheDayFile;

    final static String[] DAYS = {
            "Sonntag","Montag","Dinstag","Mittwoch","Donerstag","Freitag","Samstag"
    };

    boolean wednesday;

    private LocalDate localDate;
    private Calendar mCalendar;

    Thread t1 = new Thread(() -> StartClock());
    private int hour;
    private int min;
    private int sek;

    Calendar currentTimeCalendar = Calendar.getInstance();
    private String HourString;
    private String MinString;
    private String SekString;

    private boolean loadLessonActivated;
    private Calendar NextTimeReloadCurrentLesson = Calendar.getInstance();

    private boolean inSchool;

    private String StartTimePercentageProgressBar;
    private String EndTimePercentageProgressBar;

    Thread t2 = new Thread(() -> GetChangedPlanFromWeb());

    //UI
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

    //Stuff
    private ActivityMainBinding binding;

    //Debug
    private final boolean test = false; //ToDo: Before pushing and exporting as apk set this to false
    private final String testday = "Montag"; //Days: Sonntag, Montag, Dinstag, Mittwoch, Donerstag, Freitag, Samstag
    private boolean testTimeTick = false;
    private final int testHour = 10;
    private final int testMin = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mCalendar = Calendar.getInstance();
        localDate = LocalDate.now();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        //t2.start();

        t1.start();
        LoadPlan();
    }

    private void StartClock()
    {
        try
        {
            // create a clock
            ZoneId zid = ZoneId.of("Europe/Berlin");

            if (test)
            {
                hour = testHour;
                min = testMin;
                sek = 0;
            }

            while (true) {
                LocalDateTime lt
                        = LocalDateTime.now(zid);

                if (!test) {
                    hour = lt.getHour();
                    min = lt.getMinute();
                    sek = lt.getSecond();
                }
                else
                    if(testTimeTick)
                    {
                        if (min >= 60)
                        {
                            hour++;
                            min = 0;
                        }
                        else
                            min++;
                    }

                HourString = hour < 10 ? "0"+hour : Integer.toString(hour);
                MinString = min < 10 ? "0"+min : Integer.toString(min);
                SekString = sek < 10 ? "0"+sek : Integer.toString(sek);

                ZeitText.setText(HourString + ":" + MinString + ":" + SekString);

                String timeString = HourString + ":" + MinString + ":00";
                Date time2 = new SimpleDateFormat("HH:mm:ss").parse(timeString);
                currentTimeCalendar.setTime(time2);
                currentTimeCalendar.add(Calendar.DATE, 1);

                System.out.println("Reload Time: " + NextTimeReloadCurrentLesson.getTime());

                if (currentTimeCalendar.getTime().after(NextTimeReloadCurrentLesson.getTime()))
                {
                    loadLessonActivated = false;
                    System.out.println("Set load lesson activated false");
                }

                //gets current lesson and next lesson and shit
                if (!loadLessonActivated)
                    LoadCurrentLesson();

                //Update Progressbar
                String now;
                if (min == 60)
                    now = localDate + " " + HourString + ":59:00";
                else
                    now = localDate + " " + HourString + ":" + MinString + ":00";
                if (inSchool && StartTimePercentageProgressBar != "" && EndTimePercentageProgressBar != "")
                    LessonProgressBar.setProgress((int) GetPercentage(localDate + " " + StartTimePercentageProgressBar,localDate + " " + EndTimePercentageProgressBar,now,zid));

                try {
                    t1.sleep(1000);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void LoadPlan()
    {
        String day = GetCurrentDate();
        try
        {
            if (day == "Samstag" || day == "Sonntag")
            {
                SetTextWeekend();
                return;
            }
            else
                lessonPlanForTheDayFile = day;

            if (day == "Mittwoch")
                wednesday = true;
            else
                wednesday = false;

            InputStream is = getAssets().open(lessonPlanForTheDayFile + ".txt");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                int i = 1;
                while (reader.ready()) {
                    String line = reader.readLine();

                    String[] result = line.split(",");

                    Lesson newLesson = new Lesson(result[0],result[1]);

                    dayLessons.put(Integer.toString(i),newLesson);
                    i++;
                }
            }catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        } catch (Exception e){
            e.printStackTrace();
        }

        //ToDO: return here when changed plan is already there <- manuel download per button to download again and override
        //GetChangedPlanFromWeb();
    }

    private void GetChangedPlanFromWeb()
    {

        LoadChangedPlanIntoCurrentLessonPlan();
    }

    private void LoadChangedPlanIntoCurrentLessonPlan()
    {
        System.out.println("Test");
        try
        {
            // Open document

            String filePath = "";
            com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(filePath, "LOS-2022"); //FilePath , password
            // Decrypt PDF
            pdfDocument.decrypt();

            System.out.println("decrypt");

            com.aspose.pdf.TableAbsorber absorber = new com.aspose.pdf.TableAbsorber();

            //ToDo: Extract table and change current lesson plan accordingly

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
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void LoadCurrentLesson()
    {
        loadLessonActivated = true;
        try {
            String HourData = wednesday ? "HourDataWednesday" : "HourData";
            InputStream is = getAssets().open( HourData + ".txt");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                int lineCount = 1;
                Calendar FirstPossibleSchoolTime = Calendar.getInstance();
                Calendar LastPossibleSchoolTime = Calendar.getInstance();
                while (reader.ready()) {
                    String line = reader.readLine();

                    String[] result = line.split("-"); //z.b. 07:30-08:15 -> 07:30 & 08:15

                    String string1 = result[0] + ":00";
                    StartTimePercentageProgressBar = string1;
                    Date time1 = new SimpleDateFormat("HH:mm:ss").parse(string1);
                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.setTime(time1);
                    calendar1.add(Calendar.DATE, 1);
                    if (lineCount == 1)
                        FirstPossibleSchoolTime = calendar1;


                    String string2 = result[1] + ":00";
                    EndTimePercentageProgressBar = string2;
                    Date time2 = new SimpleDateFormat("HH:mm:ss").parse(string2);
                    Calendar calendar2 = Calendar.getInstance();
                    calendar2.setTime(time2);
                    calendar2.add(Calendar.DATE, 1);
                    if (lineCount == dayLessons.size())
                        LastPossibleSchoolTime = calendar2;

                    Date x;
                    if (test)
                    {
                        String someRandomTime = hour + ":" + min + ":00";
                        Date d = new SimpleDateFormat("HH:mm:ss").parse(someRandomTime);
                        Calendar calendar3 = Calendar.getInstance();
                        calendar3.setTime(d);
                        calendar3.add(Calendar.DATE, 1);
                        x = calendar3.getTime();
                    }
                    else
                        x = currentTimeCalendar.getTime();

                    String currentTime = HourString + ":" + MinString + ":" + SekString;

                    System.out.println("x Time:" + x.getTime());
                    if (x.after(calendar1.getTime()) && x.before(calendar2.getTime())) {
                        //checks whether the current time is between two times.

                        System.out.println("In between to times");

                        currentLesson = dayLessons.get(Integer.toString(lineCount));
                        currentHour = lineCount;

                        Lesson nextLesson;
                        if (dayLessons.containsKey(Integer.toString(lineCount+1)))
                            nextLesson = dayLessons.get(Integer.toString(lineCount+1));
                        else
                            nextLesson = new Lesson("","");

                        assert nextLesson != null;
                        SetTextLesson(result[0],result[1],nextLesson.Lesson,nextLesson.Room);

                        inSchool = true;

                        String nextLine = reader.readLine();
                        System.out.println("nextLine: " + nextLine);

                        String[] nextLineResult = nextLine.split("-");
                        String nextDate = nextLineResult[0] + ":59";
                        Date nextLineTime = new SimpleDateFormat("HH:mm:ss").parse(nextDate);
                        NextTimeReloadCurrentLesson.setTime(nextLineTime);
                        NextTimeReloadCurrentLesson.add(Calendar.DATE, 1);

                        System.out.println("Time: " + currentTime + " in between: " + string1 + "-" + string2 + "! Stunde:" + lineCount + "! Fach: " + currentLesson.Lesson + "! Next Reload Time: " + NextTimeReloadCurrentLesson.getTime() + "!");
                        return;
                    }
                    else {
                        lineCount++; //Goes to the next line with 1 added to the lineCount
                    }
                    //When no correct time is found in all lines of the hour data

                    if (x.before(FirstPossibleSchoolTime.getTime()))
                    {
                        inSchool = false;
                        SetTextBeforeSchool();
                    }
                    else if (x.after(LastPossibleSchoolTime.getTime()))
                    {
                        inSchool = false;
                        SetTextAfterSchool();
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private long GetPercentage(String dateTimeStart, String dateTimeExpiration,String dateTimeNow,ZoneId zid) {

        ZonedDateTime start = LocalDateTime.parse(dateTimeStart.replace( " " , "T" ) )
                .atZone(zid);
        ZonedDateTime end = LocalDateTime.parse(dateTimeExpiration.replace( " " , "T" ) )
                .atZone(zid);
        ZonedDateTime now = LocalDateTime.parse(dateTimeNow.replace(" ", "T") )
                .atZone(zid);

        long total = ChronoUnit.MICROS.between(start, end);
        long passed = ChronoUnit.MICROS.between(start, now);

        long percentage = passed * 100 / total;

        System.out.println("Percentage: " + String.valueOf(percentage) + " %");
        return percentage;
    }

    private String GetCurrentDate()
    {
        int dayNum = mCalendar.get(Calendar.DAY_OF_WEEK);

        String day = DAYS[dayNum - 1];

        WochentagText.setText(day);
        return day;
    }

    private void SetTextWeekend()
    {
        CurrentLessonText.setText(getResources().getString(R.string.weekend));
        De_ActivateStuffThatIsDontNeededInWeekendOrAfterSchool(false);
    }

    private void SetTextLesson(String lessonStartTime,String lessonEndTime,String nextLessonText,String nextRoomText)
    {
        CurrentLessonText.setText(currentLesson.Lesson);
        CurrentRoomText.setText(currentLesson.Room);
        CurrentHourText.setText(Integer.toString(currentHour));
        LessonStartText.setText(lessonStartTime);
        LessonEndText.setText(lessonEndTime);
        NextLessonText.setText(nextLessonText);
        NextRoomText.setText(nextRoomText);
        NextHourText.setText(Integer.toString(currentHour + 1));
        De_ActivateStuffThatIsDontNeededInWeekendOrAfterSchool(true);
    }

    private void SetTextBeforeSchool()
    {
        CurrentLessonText.setText(Objects.requireNonNull(dayLessons.get(Integer.toString(1))).Lesson);
        CurrentRoomText.setText(Objects.requireNonNull(dayLessons.get(Integer.toString(1))).Room);
        CurrentHourText.setText("1");
        LessonStartText.setText("07:30");
        String LessonEndTime = wednesday ? "08:10" : "08:15";
        LessonEndText.setText(LessonEndTime);
        NextLessonText.setText(Objects.requireNonNull(dayLessons.get(Integer.toString(2))).Lesson);
        NextRoomText.setText(Objects.requireNonNull(dayLessons.get(Integer.toString(2))).Room);
        De_ActivateStuffThatIsDontNeededInWeekendOrAfterSchool(true);
    }

    private void SetTextAfterSchool()
    {
        CurrentLessonText.setText(getResources().getString(R.string.end));
        CurrentRoomText.setText(getResources().getString(R.string.end));
        CurrentHourText.setText("");
        LessonStartText.setText("");
        LessonEndText.setText("");
        LessonProgressBar.setProgress(0);
        NextLessonText.setText("");
        NextRoomText.setText("");
        NextLessonDivider.setVisibility(View.INVISIBLE);
        NextHourDivider.setVisibility(View.INVISIBLE);
        NextTextText.setVisibility(View.INVISIBLE);
        NextHourText.setVisibility(View.INVISIBLE);
    }

    private void De_ActivateStuffThatIsDontNeededInWeekendOrAfterSchool(boolean on)
    {
        if (on)
        {
            LessonProgressBar.setVisibility(View.VISIBLE);
            NextLessonDivider.setVisibility(View.VISIBLE);
            NextHourDivider.setVisibility(View.VISIBLE);
            NextTextText.setVisibility(View.VISIBLE);
            NextHourText.setVisibility(View.VISIBLE);
        }
        else
        {
            CurrentRoomText.setText("");
            CurrentHourText.setText("");
            LessonStartText.setText("");
            LessonEndText.setText("");
            NextLessonText.setText("");
            NextRoomText.setText("");
            LessonProgressBar.setVisibility(View.INVISIBLE);
            NextLessonDivider.setVisibility(View.INVISIBLE);
            NextHourDivider.setVisibility(View.INVISIBLE);
            NextTextText.setVisibility(View.INVISIBLE);
            NextHourText.setVisibility(View.INVISIBLE);
        }
    }
}