package com.e2excel.myvoice;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.e2excel.myvoice.CustomDialogs.DeleteAccountNotificationErrorDialogFragment;
import com.e2excel.myvoice.Retrofit.ApiService;
import com.e2excel.myvoice.Retrofit.RetroClient;
import com.e2excel.myvoice.pojo.SignIn.Login;
import com.e2excel.myvoice.pojo.education_details.EduList;
import com.e2excel.myvoice.pojo.education_details.Education;
import com.e2excel.myvoice.pojo.gender_details.Gender;
import com.e2excel.myvoice.pojo.gender_details.GenderList;
import com.e2excel.myvoice.pojo.salary_details.Salary;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.e2excel.myvoice.MainActivity.Build_alert_dialog;

public class personal_info_2 extends AppCompatActivity {

    private TextView textview_gender_info, textview_education_info, textview_dob_info, textview_income_info;
    private EditText edittext_income_info;
    private SharedPreferences pref, pref2;
    private SharedPreferences.Editor editor;
    private String country_code, state_code, city_name, zip_code;
    private ApiService api;
    private String api_key;
    private Map<String, String> gender_map, education_map, salary_map;
    private ArrayList<String> gender_name_list = null, education_name_list = null, salary_name_list = null;
    private static String[] gender_name, education_name, salary_name;
    private Dialog dialog;
    private ListView listview_gender, listview_education, listview_salary;
    public String selected_gender = "not_selected", selected_education = "not_selected", selected_salary = "not_selected", selected_dob = "not_selected";
    public String gender_code, education_code;

    private String isValid = "not_initialized";

    private ProgressDialog progressDialog;

    private DatePicker datePicker;
    int mYear, mMonth, mDay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info_2);
        //getting coustom layout for toolbar-
        //have to change the menifest file too - change theme of this activity to cutomeTheme
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.dots_custom_action_bar);
        View view = getSupportActionBar().getCustomView();

        ImageView imageView = view.findViewById(R.id.dots_image_1);
        imageView.setImageDrawable(getResources().getDrawable(R.drawable.dot_2));
        ImageButton imageButton = (ImageButton) view.findViewById(R.id.action_bar_back);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Intent i = getIntent();
        country_code = i.getStringExtra("country_code");
        state_code = i.getStringExtra("state_code");
        zip_code = i.getStringExtra("zip_code");
        city_name = i.getStringExtra("city_name");

        //Toast.makeText(this, i.getStringExtra("country_name"), Toast.LENGTH_SHORT).show();

        pref = getSharedPreferences("MYVOICEAPP_PREF", MODE_PRIVATE);
        editor = pref.edit();
        pref2 = getSharedPreferences("FCM_PREF", Context.MODE_PRIVATE);

        textview_gender_info = findViewById(R.id.gender);
        textview_education_info = findViewById(R.id.highest_qualification);
        textview_dob_info = findViewById(R.id.dob);
        textview_income_info = findViewById(R.id.income);

        // Set up progress before call
        progressDialog = new ProgressDialog(personal_info_2.this);
        progressDialog.setMax(100);
        progressDialog.setMessage("Loading");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);


        api = RetroClient.getApiService();
        //update_token();
        api_key = getResources().getString(R.string.APIKEY);


    }

    public void update_token() {
        //pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //Toast.makeText(this, "email from pref: " + pref.getString("email", "not fetched from pref"), Toast.LENGTH_SHORT).show();
        ApiService api = RetroClient.getApiService();

        Call<Login> call = api.getLoginJason(pref.getString("email", null), pref.getString("password", null),
                pref2.getString("fcm_token", null), "Android", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));

        Log.d("update_token", "login called");
        progressDialog.show();

        call.enqueue(new Callback<Login>() {
            @Override
            public void onResponse(Call<Login> call, Response<Login> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    //editor = pref.edit();
                    editor.putString("token", response.body().getData().getToken());

                    editor.commit();
                    Log.d("update_token", "update token response success : " + response.body().getData().getToken());
                    Map<String, ?> allEntries = pref.getAll();
                    for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                        Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
                    }

                    //call_api_coutry();
                } else {
                    //but but i can access the error body here.,
                    try {
                        JSONObject jObjError = new JSONObject(response.errorBody().string());
                        String status = jObjError.getString("message");
                        String error_msg = jObjError.getJSONObject("data").getString("errors");
                        Build_alert_dialog(getApplicationContext(), status, error_msg);

                    } catch (Exception e) {
                        //Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Login> call, Throwable t) {
                progressDialog.dismiss();
                Build_alert_dialog(personal_info_2.this, "Connection Error", "Please Check You Internet Connection");
            }
        });


    }

    public void qualification_selection(final View view) {
        //FOR EDUCATION

        Call<Education> call2 = api.getEducationJson(api_key, "Token " + pref.getString("token", null), country_code);
        Log.d("token_detail", "used for education: " + pref.getString("token", null));
        // show it
        progressDialog.show();
        call2.enqueue(new Callback<Education>() {
            @Override
            public void onResponse(Call<Education> call, Response<Education> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    List<EduList> education_obj_list = response.body().getData().getEduList();
                    //Toast.makeText(personal_info_1.this, country_string, Toast.LENGTH_LONG).show();
                    education_map = new HashMap<>();


                    education_name_list = new ArrayList<>();

                    for (EduList g : education_obj_list) {

                        education_map.put(g.getCode(), g.getName());
                        //making an array from MAP's values
                        education_name_list.add(g.getName());


                    }
                    //printing -- LOG for testing

                    education_name = new String[education_name_list.size()];

                    for (int i = 0; i < education_name_list.size(); i++) {
                        education_name[i] = education_name_list.get(i);
                        Log.i("TAG", education_name[i]);
                    }

                    //loading the data in a view
                    if (education_name_list != null) {            //if not fatched any data than coutry filed should not be clicked; Otherwise it will be crashed.! __Rv__

                        dialog = new Dialog(personal_info_2.this);
                        dialog.setContentView(R.layout.list_view);
                        dialog.setTitle("Select Highest Qualification");
                        dialog.setCancelable(true);
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;


                        //prepare a list view in dialog
                        listview_education = dialog.findViewById(R.id.dialogList);


                        //String[] aray = {"rohn0", "fdad", "aqwe"};
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.list_item, R.id.textViewStyle, education_name);
                        listview_education.setAdapter(adapter);


                        listview_education.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                //Toast.makeText(personal_info_1.this, "Clicked Item: " + parent.getItemAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
                                view.setSelected(true);
                                selected_education = parent.getItemAtPosition(position).toString();


                                textview_education_info.setText(selected_education);

                                dialog.dismiss();

                                //taking the value of education code
                                for (Map.Entry entry : education_map.entrySet()) {
                                    if (selected_education.equals(entry.getValue())) {
                                        education_code = entry.getKey().toString();
                                        break;
                                    }
                                }

                            }
                        });


                        View view1 = dialog.findViewById(R.id.cancel_btn);
                        Button cancel_btn = view1.findViewById(R.id.cancel_btn);
                        cancel_btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });


                        dialog.show();
                    }


                } else {
                    //first chk for TOKEN EXPIRE??
                    //calling a function


                   // Toast.makeText(personal_info_2.this, "response not received", Toast.LENGTH_SHORT).show();
                    try {
                        JSONObject jObjError = new JSONObject(response.errorBody().string());
                        if (jObjError.has("detail")) {
                            if (jObjError.getString("detail").equals("Invalid Token")) {
                                update_token();
                                qualification_selection(view);

                            } else if (jObjError.getString("detail").equals("AccountDeleted")) {
                                DeleteAccountNotificationErrorDialogFragment deleteAccountNotificationErrorDialogFragment = new DeleteAccountNotificationErrorDialogFragment();
                                deleteAccountNotificationErrorDialogFragment.show(getSupportFragmentManager(), "DeleteNotificationDialogFragment");
                            }
                        }
                        else if (jObjError.has("message")) {
                            Build_alert_dialog(personal_info_2.this, jObjError.getString("message"));
                        }

                        /* String status = jObjError.getString("detail");
                         */
                        //Toast.makeText(getApplicationContext(), jObjError.toString(), Toast.LENGTH_LONG).show();

                        //Build_alert_dialog(getApplicationContext(), "Error", status);

                    } catch (Exception e) {
                        //Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Education> call, Throwable t) {
                progressDialog.dismiss();
                Build_alert_dialog(personal_info_2.this, "Connection Error", "Please Check You Internet Connection");
            }
        });
    }

    public void gender_selection(final View view) {
        //FOR GENDER
        Call<Gender> call = api.getGenderJson(api_key, "Token " + pref.getString("token", null));
        Log.d("token_detail", "used for gender: " + pref.getString("token", null));
        progressDialog.show();

        call.enqueue(new Callback<Gender>() {
            @Override
            public void onResponse(Call<Gender> call, Response<Gender> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    List<GenderList> gender_obj_list = response.body().getData().getGenderList();
                    //Toast.makeText(personal_info_1.this, country_string, Toast.LENGTH_LONG).show();
                    gender_map = new HashMap<>();


                    gender_name_list = new ArrayList<>();

                    for (GenderList g : gender_obj_list) {

                        gender_map.put(g.getCode(), g.getName());
                        //making an array from MAP's values
                        gender_name_list.add(g.getName());
                    }
                    //printing -- LOG for testing

                    gender_name = new String[gender_name_list.size()];

                    for (int i = 0; i < gender_name_list.size(); i++) {
                        gender_name[i] = gender_name_list.get(i);
                        Log.i("TAG", gender_name[i]);
                    }
                    //loading the data in a view
                    if (gender_name_list != null) {            //if not fatched any data than coutry filed should not be clicked; Otherwise it will be crashed.! __Rv__

                        dialog = new Dialog(personal_info_2.this);
                        dialog.setContentView(R.layout.list_view);
                        dialog.setTitle("Select Gender");
                        dialog.setCancelable(true);
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;


                        //prepare a list view in dialog
                        listview_gender = dialog.findViewById(R.id.dialogList);


                        //String[] aray = {"rohn0", "fdad", "aqwe"};
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.list_item, R.id.textViewStyle, gender_name);
                        listview_gender.setAdapter(adapter);


                        listview_gender.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                //Toast.makeText(personal_info_1.this, "Clicked Item: " + parent.getItemAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
                                view.setSelected(true);
                                selected_gender = parent.getItemAtPosition(position).toString();


                                textview_gender_info.setText(selected_gender);

                                dialog.dismiss();

                                //taking the value of gender code
                                for (Map.Entry entry : gender_map.entrySet()) {
                                    if (selected_gender.equals(entry.getValue())) {
                                        gender_code = entry.getKey().toString();
                                        break;
                                    }
                                }

                            }
                        });


                        View view1 = dialog.findViewById(R.id.cancel_btn);
                        Button cancel_btn = view1.findViewById(R.id.cancel_btn);
                        cancel_btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });


                        dialog.show();
                    }

                } else {
                    //first chk for TOKEN EXPIRE??
                    //calling a function


                    //Toast.makeText(personal_info_2.this, "response not received", Toast.LENGTH_SHORT).show();
                    try {
                        JSONObject jObjError = new JSONObject(response.errorBody().string());
                        if (jObjError.has("detail")) {
                            if (jObjError.getString("detail").equals("Invalid Token")) {
                                update_token();
                                gender_selection(view);
                            } else if (jObjError.getString("detail").equals("AccountDeleted")) {
                                DeleteAccountNotificationErrorDialogFragment deleteAccountNotificationErrorDialogFragment = new DeleteAccountNotificationErrorDialogFragment();
                                deleteAccountNotificationErrorDialogFragment.show(getSupportFragmentManager(), "DeleteNotificationDialogFragment");
                            }
                        }
                        else if (jObjError.has("message")) {
                            Build_alert_dialog(personal_info_2.this, jObjError.getString("message"));
                        }

                        /* String status = jObjError.getString("detail");
                         */
                        //Toast.makeText(getApplicationContext(), jObjError.toString(), Toast.LENGTH_LONG).show();

                        //Build_alert_dialog(getApplicationContext(), "Error", status);


                    } catch (Exception e) {
                        // Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Gender> call, Throwable t) {
                progressDialog.dismiss();
                Build_alert_dialog(personal_info_2.this, "Connection Error", "Please Check You Internet Connection");
            }
        });

    }

    public void dob_selection(View view) {

        // Get Current Date
        final Calendar c = Calendar.getInstance();
        if (mYear != 0) {
            c.set(Calendar.YEAR, mYear);
            c.set(Calendar.MONTH, mMonth-1);
            c.set(Calendar.DATE, mDay);

        } else {
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);

        }


        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {

                        textview_dob_info.setText((monthOfYear + 1) + "-" + dayOfMonth + "-" + year);       //for showing - format is: m-d-y
                        isValid = age_validation(dayOfMonth, monthOfYear + 1, year);
                        selected_dob = (String) (year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);        //for database_sending, format is : y-m-d

                        mYear = year;
                        mMonth = monthOfYear;
                        mDay = dayOfMonth;

                    }
                }, mYear, mMonth, mDay);

//        if (datepicker != null) {
//            datePickerDialog.updateDate(datepicker.getYear(), datepicker.getMonth() - 1, datepicker.getDayOfMonth());
//
//        }
        datePickerDialog.show();
    }

    private String age_validation(int dayOfMonth, int mon, int year) {
        Calendar today = Calendar.getInstance();
        Calendar dob = Calendar.getInstance();
        dob.set(year, mon - 1, dayOfMonth);
        //Toast.makeText(this, "today's date : " + today.get(Calendar.YEAR) + "-" + today.get(Calendar.MONTH) + "-" + today.get(Calendar.DAY_OF_YEAR), Toast.LENGTH_SHORT).show();

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if ((today.get(Calendar.MONTH) < dob.get(Calendar.MONTH)) ||
                (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) && (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)))) {
            age--;
        }
        Integer ageInt = new Integer(age);
        String ageS = ageInt.toString();

        return ageS;


    }

    public void income_selection(final View view) {
        //For INCOME
        Call<Salary> call3 = api.getSalaryJson(api_key, "Token " + pref.getString("token", null));
        Log.d("token_detail", "used for salary: " + pref.getString("token", null));
        progressDialog.show();

        call3.enqueue(new Callback<Salary>() {
            @Override
            public void onResponse(Call<Salary> call, Response<Salary> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    List<String> salary_obj_list = response.body().getData().getList();
                    //Toast.makeText(personal_info_1.this, country_string, Toast.LENGTH_LONG).show();
                    salary_map = new HashMap<>();


                    salary_name_list = new ArrayList<>();

                    for (String g : salary_obj_list) {

                        //  gender_map.put(g.getCode(), g.getName());
                        //making an array from MAP's values
                        salary_name_list.add(g);
                    }
                    //printing -- LOG for testing

                    salary_name = new String[salary_name_list.size()];

                    for (int i = 0; i < salary_name_list.size(); i++) {
                        salary_name[i] = salary_name_list.get(i);
                        Log.i("TAG", salary_name[i]);
                    }
                    //loading the data in a view
                    if (salary_name_list != null) {            //if not fatched any data than coutry filed should not be clicked; Otherwise it will be crashed.! __Rv__

                        dialog = new Dialog(personal_info_2.this);
                        dialog.setContentView(R.layout.list_view);
                        dialog.setTitle("Select Annual Income");
                        dialog.setCancelable(true);
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;


                        //prepare a list view in dialog
                        listview_salary = dialog.findViewById(R.id.dialogList);


                        //String[] aray = {"rohn0", "fdad", "aqwe"};
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.list_item, R.id.textViewStyle, salary_name);
                        listview_salary.setAdapter(adapter);


                        listview_salary.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                //Toast.makeText(personal_info_1.this, "Clicked Item: " + parent.getItemAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
                                view.setSelected(true);
                                selected_salary = parent.getItemAtPosition(position).toString();


                                textview_income_info.setText(selected_salary);

                                dialog.dismiss();


                            }
                        });


                        View view1 = dialog.findViewById(R.id.cancel_btn);
                        Button cancel_btn = view1.findViewById(R.id.cancel_btn);
                        cancel_btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });


                        dialog.show();
                    }


                } else {
                    //first chk for TOKEN EXPIRE??
                    //calling a function


                    //Toast.makeText(personal_info_2.this, "response not received", Toast.LENGTH_SHORT).show();
                    try {
                        JSONObject jObjError = new JSONObject(response.errorBody().string());
                        if (jObjError.has("detail")) {
                            if (jObjError.getString("detail").equals("Invalid Token")) {
                                update_token();
                                income_selection(view);
                            } else if (jObjError.getString("detail").equals("AccountDeleted")) {
                                DeleteAccountNotificationErrorDialogFragment deleteAccountNotificationErrorDialogFragment = new DeleteAccountNotificationErrorDialogFragment();
                                deleteAccountNotificationErrorDialogFragment.show(getSupportFragmentManager(), "DeleteNotificationDialogFragment");
                            }
                        }
                        else if (jObjError.has("message")) {
                            Build_alert_dialog(personal_info_2.this, jObjError.getString("message"));
                        }

                        /* String status = jObjError.getString("detail");
                         */
                        //Toast.makeText(getApplicationContext(), jObjError.toString(), Toast.LENGTH_LONG).show();

                        //Build_alert_dialog(getApplicationContext(), "Error", status);


                    } catch (Exception e) {
                        //Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Salary> call, Throwable t) {
                progressDialog.dismiss();
                Build_alert_dialog(personal_info_2.this, "Connection Error", "Please Check You Internet Connection");
            }
        });


    }

    public void next_activity(View view) {

        //printing toast of all values with codes
        if (selected_gender.equals("not_selected") || selected_education.equals("not_selected") || selected_dob.equals("not_selected") ||
                selected_salary.equals("not_selected")) {
            Build_alert_dialog(this, "Incomplete Details", "Please fill all the details");
        } else {
            if (Integer.parseInt(isValid) >= 13) {


                // Toast.makeText(this, "Education :" + selected_education + ", Code: " + education_code + "\n" + "gender: " + selected_gender + ", code: " + gender_code + "\n"
                // + "dob: " + selected_dob + "\n" + "income: " + selected_salary, Toast.LENGTH_SHORT).show();

                //if all ok then redirect to thenext activity
                //but first store all the gethered info. of 8 items into shared pref.

                //call to api for submitting the response.


                //start the activity of taking the permission

                Intent i = new Intent(personal_info_2.this, preference.class);
                i.putExtra("country_code", country_code);
                i.putExtra("state_code", state_code);
                i.putExtra("city_name", city_name);
                i.putExtra("zip_code", zip_code);
                i.putExtra("education_code", education_code);
                i.putExtra("gender_code", gender_code);
                i.putExtra("dob", selected_dob);
                i.putExtra("income", selected_salary);


                startActivity(i);


            } else {
                Build_alert_dialog(this, "Age restriction", "you must be 13+");
                selected_dob = "not_selected";
            }

        }


    }


}
