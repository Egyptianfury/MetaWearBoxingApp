package ca.concordia.metaweargpioexample;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class HistoryGraph extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_graph);
        BarChart chart = (BarChart) findViewById(R.id.chart);
        ProfilePage.myDb = new DatabaseHelper(this);

        BarData data = new BarData(getXAxisValues(), getDataSet());
        chart.setData(data);
        chart.setDescription("History of Frequency");
        chart.animateXY(2000, 2000);
        chart.invalidate();
    }

    private ArrayList<BarDataSet> getDataSet() {
        ArrayList<BarDataSet> dataSets = null;

        int count = 0;
        ArrayList<BarEntry> entries = new ArrayList<>();
        Cursor res = ProfilePage.myDb.getAllFreq();
        while (res.moveToNext()) {
            entries.add(new BarEntry(Float.parseFloat(res.getString(1)), count));
            count++;
        }

        BarDataSet barDataSet1 = new BarDataSet(entries, "Brand 1");
        barDataSet1.setColors(ColorTemplate.COLORFUL_COLORS);

        dataSets = new ArrayList<>();
        dataSets.add(barDataSet1);
        return dataSets;
    }

    private ArrayList<String> getXAxisValues() {
        ArrayList<String> xAxis = new ArrayList<>();
        Cursor res = ProfilePage.myDb.getAllFreq();
        while (res.moveToNext()) {
            xAxis.add(res.getString(2));
        }
        return xAxis;
    }

}