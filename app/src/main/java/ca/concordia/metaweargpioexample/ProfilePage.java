package ca.concordia.metaweargpioexample;

import android.app.AlertDialog;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ProfilePage extends AppCompatActivity {
    static DatabaseHelper myDb;
    EditText editFirst, editLast, editWeight, editLead, editGym;
    Button savedata;
    Button viewAllData;
    Button dropDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);
        myDb = new DatabaseHelper(this);
        savedata = (Button) findViewById(R.id.saveEdits);
        viewAllData = (Button) findViewById(R.id.viewData);
        dropDb = (Button) findViewById(R.id.deleteData);
        editFirst = (EditText) findViewById(R.id.firstEdit);
        editLast = (EditText) findViewById(R.id.lastEdit);
        editWeight = (EditText) findViewById(R.id.weightEdit);
        editLead = (EditText) findViewById(R.id.leadEdit);
        editGym = (EditText) findViewById(R.id.gymEdit);
        AddData();
        viewAll();
        dropTable();
    }

    public void AddData(){
        savedata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                if (editLead.getText().toString().equalsIgnoreCase("left") || editLead.getText().toString().equalsIgnoreCase("right")) {
                    myDb.delDatabase();
                    boolean isInserted = myDb.insertData(editFirst.getText().toString(), editLast.getText().toString(), editWeight.getText().toString(), editGym.getText().toString(), editLead.getText().toString());
                    if (isInserted)
                        Toast.makeText(ProfilePage.this, "Data Saved", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(ProfilePage.this, "Data Not Saved", Toast.LENGTH_LONG).show();
                }
                    else {
                    Toast.makeText(ProfilePage.this, "Lead Must be either Left or Right", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void dropTable(){
        dropDb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myDb.delDatabase();
                Toast.makeText(ProfilePage.this, "User Data Has Been Deleted", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void showMessage(String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    public void viewAll(){
        viewAllData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Cursor res = myDb.getAllData();
                if (res.getCount() == 0){
                    showMessage("Error", "No User Data");
                    return;
                }
                StringBuffer buffer = new StringBuffer();
                while (res.moveToNext()){
                    buffer.append("ID: " + res.getString(0) + "\n");
                    buffer.append("First Name: " + res.getString(1) + "\n");
                    buffer.append("Last Name: " + res.getString(2) + "\n");
                    buffer.append("Weight: " + res.getString(3) + "\n");
                    buffer.append("Gym: " + res.getString(4) + "\n");
                    buffer.append("Lead Hand: " + res.getString(5) + "\n\n");
                }
                showMessage("User Data", buffer.toString());
            }
        });
    }
}
