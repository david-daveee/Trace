<p align="center">
  <img src="docs/trace-logo.svg" alt="Trace logo" width="112" height="112">
</p>

# Trace

**Your training plan, always within reach.**

Train without opening a spreadsheet. Trace shows your next set, calculates your weights, and keeps your workout history. Log sets in the app or use the workout controls in your phone's system panel.

**Android 8.0+ · English / Russian · No account required · Works offline**

## Download and install

### [Download Trace for Android — APK](https://github.com/david-daveee/Trace/releases/latest/download/Trace.apk)

[All releases and release notes](https://github.com/david-daveee/Trace/releases)

1. Open this page **on your Android phone** and tap **Download Trace for Android**.
2. Open **Trace.apk** from your browser's downloads or your Downloads folder.
3. If Android asks you to allow installation from this source, open the suggested settings and allow it for the browser or file manager you used to open the APK. The exact wording varies by phone.
4. Return to the file, tap **Install**, then **Open**.
5. When you start your first workout, allow notifications to use the system workout controls.

You do not need Android Studio or a computer. An iPhone version is not available yet.

**To switch the app to English:** tap the settings icon at the top and select **English** in the language selector.

## Start your first workout

1. Open **Plans**, choose a program, and tap **Add to my plans**.
2. Enter your weights in the program card. Sheiko plans use your **squat, bench press, and deadlift maxes** to calculate percentages. The Zlat plan uses **added working weight** for pull-ups and dips.
3. Open your program in **My plans** and start a workout.
4. Complete a set and mark it done. The next set appears immediately; there is no rest timer.
5. At the end, tap **Save workout**. Your results appear in history and progress, and the next session advances to the next day.

The default maxes in Sheiko plans come from the source spreadsheets. Replace them with your own before starting. For accessory exercises without a prescribed weight, enter your working weight manually.

<img src="docs/trace-plan.png" alt="A Sheiko plan in Trace with program maxes and a workout schedule" width="320">

## Find your way around

| Tab | What you can do |
| --- | --- |
| **Plans** | Browse ready-made programs, import a spreadsheet, or create a custom plan. |
| **My plans** | Find your programs, change weights, and continue training. Your most recently used program appears first. |
| **Workout** | Review sets, edit weight and reps, and mark or unmark any set. |
| **Progress** | View your activity calendar, stats, and saved workout history. |
| **Steps** | Track today's steps with your phone's sensor and set a daily goal. |

## Today's steps

Open **Steps → Turn on tracking** and allow physical activity access. Carry your phone with you to record steps, including with the screen off. The daily goal starts at 8,000 and can be changed with **Edit goal**.

The Steps tab and tracking notification also show **estimated distance in kilometers**. Tap **Step length** to adjust the default of 70 cm. Distance is calculated from step count and step length, not GPS. For calibration, divide a known walking distance in centimeters by the number of steps you took.

In **Progress**, the **Your daily rhythm** card shows today's steps and estimated distance, an interactive seven-day chart, and weekly and all-time totals. Tap a bar to view that day's numbers. A dash means no records; walking stays separate from your saved workout calendar. Distance estimates use your current step length.

Tracking begins when you enable it; steps from earlier today cannot be recovered. A quiet notification keeps background tracking visible. After a reboot or force-stop, open Trace again. Android may interrupt background work, so some steps can be missed. A built-in step counter is required; GPS is not used.

Step totals are stored separately on the phone and are not yet included in workout backups.

## During your workout

- **Keep controls within reach.** Pull down your phone's system panel to see the current exercise and set actions. Its appearance depends on your Android version and phone software.
- **Fix an accidental tap.** Unmark any set in the Workout tab. Undo removes the most recent completion mark.
- **Update a max once.** Future percentage-based weights update throughout that program. Completed sets keep their recorded weights.
- **Switch programs freely.** Your unfinished session stays paused so you can return to it later.
- **Keep your list tidy.** Use **Remove from my plans** to hide a program from your list. Its history stays, and you can add it again from Plans.

## Included programs

| Program | What's inside |
| --- | --- |
| **Sheiko · CMS / MS** | A four-week preparation cycle with 16 workouts. |
| **Sheiko · 12 weeks to competition** | Eight weeks of preparation and a four-week competition phase: 35 workouts followed by a competition event. |
| **Matvey Zlat · pull-ups and dips** | A basic weighted routine from 2018. Next-session increases are calculated after you confirm your results. |

Sheiko weights follow the percentages prescribed in each plan. Zlat increases depend on the result of the final planned set of each exercise.

## Bring your own plan

Open **Plans** to import a file or choose **Custom plan**.

Trace supports JSON, CSV and XLSX using the in-app template, plus the supported original Sheiko XLS format. You can download the template from the app. Arbitrarily formatted Excel workbooks are not automatically recognized; the 12-week Sheiko cycle is already included separately in the catalog.

### Make a plan yours

Open a plan and tap **Change cover → Choose photo or screenshot**. Pick an image from your phone, including the Screenshots folder. Drag to reposition it, pinch or use the zoom slider, then tap **Save**. To adjust it later, choose **Change cover → Adjust cover**. Cancel leaves your previous cover unchanged. Trace keeps an optimized copy, so the cover stays available after restarting and travels with JSON exports and workout backups. Use **Reset cover** to restore the default image.

To permanently remove a custom or imported plan, tap **Delete plan** in Plans or at the bottom of its detail page and confirm. This removes the plan from both lists and discards its unfinished session. Completed workout history stays. Built-in catalog plans remain available; **Remove from my plans** simply hides them from your personal list.

## Update Trace

Download the latest **Trace.apk** using the link above and install it **over your current version**. You do not need to uninstall Trace first.

Before updating, save a backup through **Settings → Save backup**. It includes your programs, history, and unfinished workout.

## Frequently asked questions

**Where is my data stored?**  
On your phone. There is no account or cloud sync. To move to another phone, save a backup, transfer the file, and choose **Settings → Restore from file** on the new device. Restoring replaces its current data after confirmation.

**Why isn't my workout showing in Progress?**  
The calendar counts saved workouts. After completing all sets, tap **Save workout**.

**How do I change the language?**  
Tap the settings icon at the top and choose English or Russian.

**Why can't I see the workout panel?**  
Start a workout and check that notifications are enabled for Trace in Android settings. If the app was force-stopped, open it again. Lock-screen visibility depends on your phone settings.

**Android says “App not installed.”**  
Check that the download finished and your phone has enough free space. An existing Trace build signed with a different key may prevent the update from installing. Save a backup first; do not uninstall the only copy of your workout history.

## Have an idea or found a problem?

[Open an issue](https://github.com/david-daveee/Trace/issues) with your phone model, Android version, and what happened. A screenshot without personal information can help explain a bug.

<details>
<summary>For developers</summary>

Java 17, Android Views, and Android SDK 35. Minimum Android version: 8.0 (API 26).

Open the project root in Android Studio and wait for Gradle sync. Build and run checks on Windows:

```powershell
./gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`. The downloadable build uses a debug signing key and is distributed through GitHub Releases.

[Implementation details and development notes](docs/DEVELOPMENT.md).

</details>
