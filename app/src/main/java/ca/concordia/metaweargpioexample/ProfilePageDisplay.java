package ca.concordia.metaweargpioexample;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class ProfilePageDisplay extends AppCompatActivity {

    EditText setFirst, setLast, setWeight, setGym, setLead, setID;
    public static final int RESULT_LOAD_IMAGE = 1;
    ImageView imageToUpload;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page_display);

        setFirst = (EditText) findViewById(R.id.set_first);
        setLast = (EditText) findViewById(R.id.set_last);
        setWeight = (EditText) findViewById(R.id.set_weight);
        setGym = (EditText) findViewById(R.id.set_gym);
        setLead = (EditText) findViewById(R.id.set_lead);
        setID = (EditText) findViewById(R.id.set_id);
        imageToUpload = (ImageView) findViewById(R.id.imageToUpload);

        Button editUserinfo = (Button) findViewById(R.id.editUserInfo);
        editUserinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfilePageDisplay.this, ProfilePage.class);
                startActivity(intent);
            }
        });
        pickPicture();
        setUserData();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            imageToUpload.setImageURI(selectedImage);
        }
    }


    public void setUserData(){
        Cursor res = ProfilePage.myDb.getAllData();
        if (res.getCount() == 0){
            setID.setText(null);
            setFirst.setText(null);
            setLast.setText(null);
            setWeight.setText(null);
            setGym.setText(null);
            setLead.setText(null);
            return;
        }
        else
            res.moveToNext();
            setID.setText(res.getString(0));
            setFirst.setText(res.getString(1).substring(0,1).toUpperCase() + res.getString(1).substring(1));
            setLast.setText(res.getString(2).substring(0, 1).toUpperCase() + res.getString(2).substring(1));
            setWeight.setText(res.getString(3).substring(0,1).toUpperCase() + res.getString(3).substring(1));
            setGym.setText(res.getString(4).substring(0,1).toUpperCase() + res.getString(4).substring(1));
            setLead.setText(res.getString(5).substring(0, 1).toUpperCase() + res.getString(5).substring(1));
    }


    @Override
    public void onResume(){
        super.onResume();
        setUserData();
    }


    public void pickPicture() {
        findViewById(R.id.select_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
            }
        });
    }
}



