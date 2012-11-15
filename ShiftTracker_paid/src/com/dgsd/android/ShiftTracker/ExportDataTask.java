package com.dgsd.android.ShiftTracker;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.text.format.Time;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVWriter;
import com.dgsd.android.ShiftTracker.Data.DbField;
import com.dgsd.android.ShiftTracker.Data.InternalFileProvider;
import com.dgsd.android.ShiftTracker.Data.Provider;
import com.dgsd.android.ShiftTracker.Model.Shift;
import com.dgsd.android.ShiftTracker.Util.ProviderUtils;
import com.dgsd.android.ShiftTracker.Util.TimeUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class ExportDataTask extends AsyncTask<Void, Integer, String> {

    private PreferenceActivity mActivity;
    private ProgressDialog mProgressDialog;

    private boolean mHasNoShifts = false;

    public static ExportDataTask start(PreferenceActivity activity) {
        ExportDataTask task = new ExportDataTask(activity);
        task.execute(null, null, null);

        return task;
    }

    private ExportDataTask(PreferenceActivity activity) {
        mActivity = activity;
        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setMessage(mActivity.getString(R.string.exporting_data));
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setProgressNumberFormat(null);
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog.show();
    }

    @Override
    protected String doInBackground(Void... voids) {
        List<Shift> shifts = getShifts();

        if (shifts == null || shifts.isEmpty())
            return null;

        try {
            Time time = new Time();
            time.setToNow();

            String filename = mActivity.getString(R.string.app_name) + " export - " + time.format2445() + ".csv";
            CSVWriter writer = new CSVWriter(new FileWriter(getFilePath(filename)));

            writer.writeNext(new String[]{
                "name",
                "notes",
                "start_time",
                "end_time",
                "julian_day",
                "pay_rate",
                "break_duration"
            });

            for (int i = 0, size = shifts.size(); i < size; i++) {
                final Shift shift = shifts.get(i);
                String[] values = {
                    shift.name,
                    shift.note,
                    shift.getStartTime() == -1 ? null : String.valueOf(shift.getStartTime()),
                    shift.getEndTime() == -1 ? null : String.valueOf(shift.getEndTime()),
                    shift.julianDay == -1 ? null : String.valueOf(shift.julianDay),
                    shift.payRate == -1 ? null : String.valueOf(shift.payRate),
                    shift.breakDuration == -1 ? null : String.valueOf(shift.breakDuration)
                };

                writer.writeNext(values);
                publishProgress((size / 2) + (i / 2), size);
            }

            writer.close();

            return filename;
        } catch (IOException e) {
            //O well
        }

        return null;
    }

    private List<Shift> getShifts() {
        List<Shift> shifts = null;
        Cursor cursor = null;
        try {
            String sort = DbField.JULIAN_DAY + " ASC, " + DbField.START_TIME + " ASC, " + DbField.NAME + " ASC";
            cursor = ProviderUtils.doQuery(mActivity, Provider.SHIFTS_URI, null, null, null, sort);
            if (cursor != null && cursor.moveToFirst()) {
                shifts = new ArrayList<Shift>(cursor.getCount());
                do {
                    shifts.add(Shift.fromCursor(cursor));
                    publishProgress(cursor.getPosition() / 2, cursor.getCount());
                } while (cursor.moveToNext());
            } else {
                mHasNoShifts = true;
            }
        } catch (Exception e) {
            shifts = null;
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return shifts;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        mProgressDialog.setProgress(values[0]);
        mProgressDialog.setMax(values[1]);
    }

    @Override
    protected void onPostExecute(String fileName) {
        if (mProgressDialog.isShowing())
            mProgressDialog.dismiss();

        if(mActivity == null)
            return;

        if(TextUtils.isEmpty(fileName)) {
            if(mHasNoShifts)
                Toast.makeText(mActivity, "Couldn't find any shifts to export", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(mActivity, "Error exporting data", Toast.LENGTH_SHORT).show();
            return;
        }

        //Great, we have our filename now .. create the intent!
        final String subject = mActivity.getString(R.string.app_name) + " export";
        final String msg = subject + " created on " + DateFormat.getDateTimeInstance().format(TimeUtils.getCurrentMillis());
        final Uri fileUri = Uri.parse("content://" + InternalFileProvider.AUTHORITY + "/" + fileName);

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, msg);
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);

        mActivity.startActivity(intent);
    }

    private String getFilePath(String fileName) throws IOException {
        File cacheFile = new File(mActivity.getCacheDir() + File.separator + fileName);
        if (!cacheFile.exists())
            if (!cacheFile.createNewFile())
                throw new IOException("Unable to create file");

        return cacheFile.getAbsolutePath();
    }

}
