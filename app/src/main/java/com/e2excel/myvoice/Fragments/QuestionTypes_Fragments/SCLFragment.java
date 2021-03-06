package com.e2excel.myvoice.Fragments.QuestionTypes_Fragments;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.e2excel.myvoice.CustomDialogs.DeleteAccountNotificationErrorDialogFragment;
import com.e2excel.myvoice.Fragments.QuestionsListFragment;
import com.e2excel.myvoice.GlobalValues.PublicClass;
import com.e2excel.myvoice.R;
import com.e2excel.myvoice.Retrofit.ApiService;
import com.e2excel.myvoice.Retrofit.RetroClient;
import com.e2excel.myvoice.pojo.Response.response;
import com.e2excel.myvoice.pojo.SignIn.Login;
import com.e2excel.myvoice.pojo.survey_question_detail_SCL.Data;
import com.e2excel.myvoice.pojo.survey_question_detail_SCL.QuestionDetail;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.e2excel.myvoice.MainActivity.Build_alert_dialog;

/**
 * A simple {@link Fragment} subclass.
 */
public class SCLFragment extends Fragment {
    private static String MEDIA = "false";
    View v;
    private String q_id, q_text, response_text = "";
    private ProgressDialog progressDialog;

    private TextView que, current, total;
    private FrameLayout frameLayout;
    private ImageView imageView;
    private WebView webView;
    private LinearLayout linearLayout;


    private AppCompatSeekBar seekbar;

    ApiService api;
    String api_key;
    private SharedPreferences pref, pref2;
    private SharedPreferences.Editor editor;
    private double min_double, max_double, step_double;
    private int min_int, max_int, step_int;
    private Number min_num, max_num, step_num;
    private int step_count = 1;

    private Button submit_button;

    Boolean DOUBLE_VALUE = false;
    Boolean NEGATIVE_VALUE = false;


    private Data data;

    public SCLFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        v = inflater.inflate(R.layout.fragment_scale, container, false);


        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);

        TextView t = toolbar.findViewById(R.id.title_text);
        t.setText("Question");

        ImageView back = toolbar.findViewById(R.id.back_image);
        back.setVisibility(View.VISIBLE);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction().replace(R.id.framelayout_container, new QuestionsListFragment()).commit();
            }
        });

        TextView logout_btn = toolbar.findViewById(R.id.logout_textview);
        logout_btn.setVisibility(View.INVISIBLE);

        que = v.findViewById(R.id.full_que);
        frameLayout = v.findViewById(R.id.frame_view);
        imageView = v.findViewById(R.id.image_view);
        webView = v.findViewById(R.id.web_view);
        submit_button = v.findViewById(R.id.submit_btn);
        linearLayout = v.findViewById(R.id.linear_layout);
        seekbar = v.findViewById(R.id.appCompatSeekBar);

        current = v.findViewById(R.id.current_num);
        total = v.findViewById(R.id.total_num);

        //option_list_view = v.findViewById(R.id.options_list);

        //intially visibility is gone
        frameLayout.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.INVISIBLE);
        webView.setVisibility(View.INVISIBLE);
        submit_button = v.findViewById(R.id.submit_btn);


        Bundle bundle = this.getArguments();
        q_id = bundle.get("q_id").toString();
        //q_text = bundle.get("q_text").toString();

        progressDialog = new ProgressDialog(this.getActivity());
        progressDialog.setMax(100);

        progressDialog.setMessage("Loading");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //when ever the question detail frag is there i increses the value more then 1
        PublicClass.CURRENT_FRAG = 2;

        pref = this.getActivity().getSharedPreferences("MYVOICEAPP_PREF", Context.MODE_PRIVATE);
        editor = pref.edit();
        pref2 = this.getActivity().getSharedPreferences("FCM_PREF", Context.MODE_PRIVATE);

        api_key = getResources().getString(R.string.APIKEY);

        api = RetroClient.getApiService();

        //que.setText(q_text);          //q_text

        load_question();


        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (DOUBLE_VALUE) {
                    double current_progress = (progress * step_double) + min_double;

                    String current_string = String.format("%.2f", current_progress);

                    Log.v("seekbar", "current (in double) " + current_string);

                    current.setText(current_string);
                } else {
                    //int logic
                    int progress2 = (progress * step_int) + min_int;
                    current.setText(String.valueOf(progress2));
                    Log.v("seekbar", "current (in int) " + current.getText());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        submit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                submit_call();

            }
        });

    }

    private void submit_call() {
        //ANs is -> current.getText();
        Log.v("seekbar", "value to be submitted: " + current.getText().toString().trim());
        JSONObject jo = new JSONObject();
        JSONArray ja = new JSONArray();

        try {
            jo.put("Key", "SCL");
            jo.put("Value", current.getText().toString().trim());

            ja.put(jo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.v("final_json", ja.toString());
        Log.v("seekbar", "JSOn response: " + ja.toString());

        Log.v("data", "attribute id: " + data.getAttributeID().toString());
        Log.v("data", "que ID: " + data.getQuestionID().toString());
        Log.v("data", "parent id: " + data.getParentID().toString());
        Log.v("data", "main parent id: " + PublicClass.MainParentID.toString().trim());


        if (ja.length() > 0) {

            Call<response> call1 = api.getMCQResponseJson(api_key, "Token " + pref.getString("token", null),
                    data.getAttributeID().toString(), data.getQuestionID().toString(),
                    data.getParentID().toString(), ja,
                    "Android", PublicClass.MainParentID.trim());

            if(!((Activity) getActivity()).isFinishing())
            {
                //show dialog
                progressDialog.show();
            }

            call1.enqueue(new Callback<response>() {
                @Override
                public void onResponse(Call<response> call, Response<response> response) {
                    if (response.isSuccessful() && "Success".equals(response.body().getStatus())) {
                        progressDialog.dismiss();


                        try {
                            JSONObject jObjError = new JSONObject(response.errorBody().string());

                            if (jObjError.has("detail")) {
                                if (jObjError.getString("detail").equals("Invalid Token")) {
                                    update_token_submit();

                                }
                                else if (jObjError.getString("detail").equals("AccountDeleted")) {
                                    DeleteAccountNotificationErrorDialogFragment deleteAccountNotificationErrorDialogFragment = new DeleteAccountNotificationErrorDialogFragment();
                                    deleteAccountNotificationErrorDialogFragment.show(getFragmentManager(), "DeleteNotificationDialogFragment");
                                }
                            }
                            else if (jObjError.has("message")) {
                                Build_alert_dialog(getActivity(), jObjError.getString("message"));
                            }
                        } catch (Exception e) {

                        }
                        //if IsNext = No
                        if ("No".equals(response.body().getIsNext())) {
                            progressDialog.dismiss();
                            Toast toast = new Toast(getActivity());
                            toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0,20);
                            LayoutInflater inflater = getActivity().getLayoutInflater();



                            View toastLayout = inflater.inflate(R.layout.custom_toast,
                                    null);

                            TextView  view1=(TextView)toastLayout.findViewById(R.id.toast_text);
                            view1.setText(response.body().getMessage());

                            toast.setDuration(Toast.LENGTH_SHORT);
                            toast.setView(toastLayout);
                            toast.show();
                            Log.v("test", "from SCL: response.body().getIsNext()" + response.body().getIsNext());
                            getFragmentManager().beginTransaction().replace(R.id.framelayout_container, new QuestionsListFragment()).commit();

                        } else {
                            progressDialog.dismiss();
                            //if IsNext = Yes
                            //there are children question(s)...we got id and and question type from the response.
                            AppCompatActivity activity = (AppCompatActivity) v.getContext();
                            Fragment myFragment = null;
                            String q_type = String.valueOf(response.body().getQuestionType());
                            Log.v("test", "from MCQ: response.body().getIsNext()-(before switch)" + String.valueOf(response.body().getQuestionType()));

                            switch (q_type) {
                                case "SCQ": {
                                    myFragment = new SCQFragment();
                                    break;
                                }
                                case "MCQ": {
                                    myFragment = new MCQFragment();
                                    break;
                                }
                                case "OTT": {
                                    myFragment = new OTTFragment();
                                    break;
                                }
                                case "SCL": {
                                    myFragment = new SCLFragment();
                                    break;
                                }
                                case "RNK": {
                                    myFragment = new RNKFragment();
                                    break;
                                }
                                case "OTN": {
                                    myFragment = new OTNFragment();
                                    break;
                                }
                            }
                            Bundle b = new Bundle();
                            //b.putString("q_text", mdata.get(getPosition()).getQuestionText());
                            b.putString("q_id", String.valueOf(response.body().getQuestionID()));
                            myFragment.setArguments(b);

                            Log.v("test", "from SCL: redirect to the new fragmnent :" + String.valueOf(response.body().getQuestionType()));

                            activity.getSupportFragmentManager().beginTransaction().replace(R.id.framelayout_container, myFragment).commit();
                        }
                    }
                }

                @Override
                public void onFailure(Call<response> call, Throwable t) {
                    progressDialog.dismiss();
                }
            });
        }

    }

    public void load_question() {
        Call<QuestionDetail> call = api.getSCLJson(api_key, "Token " + pref.getString("token", null), q_id);

        if(!((Activity) getActivity()).isFinishing())
        {
            //show dialog
            progressDialog.show();
        }

        call.enqueue(new Callback<QuestionDetail>() {
            @Override
            public void onResponse(Call<QuestionDetail> call, Response<QuestionDetail> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body().getStatus().equals("Sucess")) {

                    //display the question
                    que.setText(response.body().getData().getQuestionText());

                    if ("true".equals(response.body().getData().getQuestionIsMedia().toString())) {

                        MEDIA = "true";
                        //checking and loading for image audio or video
                        if (!"".equals(response.body().getData().getQuestionMedia()
                        )) {
                            frameLayout.setVisibility(View.VISIBLE);
                            imageView.setVisibility(View.VISIBLE);
                            Glide.with(v).load(response.body().getData().getQuestionMedia()).into(imageView);
                        } else if (!"".equals(response.body().getData().getQuestionVideoMedia())) {
                            frameLayout.setVisibility(View.VISIBLE);
                            webView.setVisibility(View.VISIBLE);
                            //Enable Javascript
                            WebSettings webSettings = webView.getSettings();
                            webSettings.setJavaScriptEnabled(true);
                            webView.setWebViewClient(new WebViewClient());      //to load content inside the webview rather then open it in browser
                            webView.loadUrl(response.body().getData().getQuestionVideoMedia());
                        } else if (!"".equals(response.body().getData().getQuestionAudioMedia())) {
                            frameLayout.setVisibility(View.VISIBLE);
                            webView.setVisibility(View.VISIBLE);
                            // Enable Javascript
                            WebSettings webSettings = webView.getSettings();
                            webSettings.setJavaScriptEnabled(true);
                            webView.setWebViewClient(new WebViewClient());      //to load content inside the webview rather then open it in browser
                            webView.loadUrl(response.body().getData().getQuestionAudioMedia());
                        }
                    } else {
                        //remove top constraint of liner layout of (3 out of 10) from frame_view bottom to full_que bottom
                        ConstraintLayout constraintLayout = v.findViewById(R.id.constraint_layout);
                        ConstraintSet constraintSet = new ConstraintSet();

                        constraintSet.clone(constraintLayout);

                        constraintSet.connect(linearLayout.getId(), constraintSet.TOP, que.getId(), constraintSet.BOTTOM, 64);

                        constraintSet.applyTo(constraintLayout);

                    }
                    current.setText(response.body().getData().getQuestionOptions().getMin());
                    total.setText(response.body().getData().getQuestionOptions().getMax());

                    //if max, min and step value we receive from response contain .(i.e its double type of value) OR contain -(i.e negative value then I use logic of DOUBLE_VALUE true)
                    if (response.body().getData().getQuestionOptions().getMax().contains(".") ||
                            response.body().getData().getQuestionOptions().getMin().contains(".") ||
                            response.body().getData().getQuestionOptions().getStep().contains(".")) {
                        DOUBLE_VALUE = true;
                    }

                    if (response.body().getData().getQuestionOptions().getMax().contains("-") ||
                            response.body().getData().getQuestionOptions().getMin().contains("-") ||
                            response.body().getData().getQuestionOptions().getStep().contains("-")) {
                        NEGATIVE_VALUE = true;
                    }
                    //seekbar.incrementProgressBy();

                    //get the data object for submit use
                    data = response.body().getData();

                    //if min value is int and all the values are not float (i.e all are int) then...
                    if (!response.body().getData().getQuestionOptions().getMax().contains(".") &&
                            !response.body().getData().getQuestionOptions().getMin().contains(".") &&
                            !response.body().getData().getQuestionOptions().getStep().contains(".") &&
                            "0".equals(response.body().getData().getQuestionOptions().getMin())) {
                        DOUBLE_VALUE = false;
                        NEGATIVE_VALUE = false;

                    }

                    //if DOUBLE
                    if (DOUBLE_VALUE) {

                        max_double = Double.valueOf(response.body().getData().getQuestionOptions().getMax());
                        min_double = Double.valueOf(response.body().getData().getQuestionOptions().getMin());
                        step_double = Double.valueOf(response.body().getData().getQuestionOptions().getStep());

                        double temp = min_double;
                        String temp_string;
                        while (temp <= max_double) {
                            step_count++;
                            temp = temp + step_double;
                            temp_string = String.format("%.2f", temp);
                            temp = Float.valueOf(temp_string);
                        }
                        step_count -= 2;      //because we have started from 0
                        Log.v("seekbar", "step: " + String.valueOf(step_count));
                        seekbar.setMax(step_count);

                    } else {
                        //it's int



                        max_int = Integer.valueOf(response.body().getData().getQuestionOptions().getMax());
                        min_int = Integer.valueOf(response.body().getData().getQuestionOptions().getMin());
                        step_int = Integer.valueOf(response.body().getData().getQuestionOptions().getStep());

                        int temp2 = min_int;

                        while(temp2<=max_int){
                            step_count++;
                            temp2 = temp2 + step_int;
                        }
                        step_count -= 2;      //because we have started from 0
                        Log.v("all_log", "int step count: "+step_count);

                        seekbar.setMax(step_count);

                        //seekbar.setMax(max_int);
                       // seekbar.incrementProgressBy(step_int);
                        //seekbar.setProgress(min_int);
                    }
                } else {
                    try {
                        JSONObject jObjError = new JSONObject(response.errorBody().string());
                        /* String status = jObjError.getString("detail");
                         */
                        //call update token function only when Error is "Invalid token" received form the server
                        if (jObjError.has("detail")) {
                            if (jObjError.getString("detail").equals("Invalid Token")) {
                                update_token_que();
                            } else if (jObjError.getString("detail").equals("AccountDeleted")) {
                                DeleteAccountNotificationErrorDialogFragment deleteAccountNotificationErrorDialogFragment = new DeleteAccountNotificationErrorDialogFragment();
                                deleteAccountNotificationErrorDialogFragment.show(getFragmentManager(), "DeleteNotificationDialogFragment");
                            }
                        }
                        else if (jObjError.has("message")) {
                            Build_alert_dialog(getActivity(), jObjError.getString("message"));
                        }

                    } catch (Exception e) {
                        //Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<QuestionDetail> call, Throwable t) {
                progressDialog.dismiss();
            }
        });
    }

    private void update_token_submit() {
        ApiService api = RetroClient.getApiService();

        //if fcm token is null then do not write in shared pref!
       /* if (PublicClass.FCM_TOKEN != null) {
            editor.putString("fcm_token", PublicClass.FCM_TOKEN);
            editor.commit();
        }*/

        Call<Login> call = api.getLoginJason(pref.getString("email", null), pref.getString("password", null),
                pref2.getString("fcm_token", null),
                "Android", Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID));

        if(!((Activity) getActivity()).isFinishing())
        {
            //show dialog
            progressDialog.show();
        }

        call.enqueue(new Callback<Login>() {
            @Override
            public void onResponse(Call<Login> call, Response<Login> response) {
                progressDialog.dismiss();

                if (response.isSuccessful()) {
                    //editor = pref.edit();
                    editor.putString("token", response.body().getData().getToken());

                    editor.commit();
                    Log.d("token", "Token " + pref.getString("token", null));

                    Map<String, ?> allEntries = pref.getAll();
                    for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                        Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
                    }

                    submit_call();
                    //call_api_coutry();
                }
            }

            @Override
            public void onFailure(Call<Login> call, Throwable t) {
                progressDialog.dismiss();
                //Build_alert_dialog(getActivity(), "Connection Error", "Please Check You Internet Connection");
            }
        });
    }

    private void update_token_que() {
        ApiService api = RetroClient.getApiService();

        //if fcm token is null then do not write in shared pref!
       /* if (PublicClass.FCM_TOKEN != null) {
            editor.putString("fcm_token", PublicClass.FCM_TOKEN);
            editor.commit();
        }*/

        Call<Login> call = api.getLoginJason(pref.getString("email", null), pref.getString("password", null),
               pref2.getString("fcm_token", null),
                "Android", Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID));

        if(!((Activity) getActivity()).isFinishing())
        {
            //show dialog
            progressDialog.show();
        }

        call.enqueue(new Callback<Login>() {
            @Override
            public void onResponse(Call<Login> call, Response<Login> response) {
                progressDialog.dismiss();

                if (response.isSuccessful()) {
                    //editor = pref.edit();
                    editor.putString("token", response.body().getData().getToken());

                    editor.commit();
                    Log.d("token", "Token " + pref.getString("token", null));

                    Map<String, ?> allEntries = pref.getAll();
                    for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                        Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
                    }

                    load_question();
                    //call_api_coutry();
                }

            }

            @Override
            public void onFailure(Call<Login> call, Throwable t) {
                progressDialog.dismiss();
                //Build_alert_dialog(getActivity(), "Connection Error", "Please Check You Internet Connection");
            }
        });

    }
}
