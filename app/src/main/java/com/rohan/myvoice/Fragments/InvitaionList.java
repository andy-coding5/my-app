package com.rohan.myvoice.Fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.rohan.myvoice.GlobalValues.PublicClass;
import com.rohan.myvoice.R;
import com.rohan.myvoice.Retrofit.ApiService;
import com.rohan.myvoice.Retrofit.RetroClient;
import com.rohan.myvoice.pojo.SignIn.Login;
import com.rohan.myvoice.pojo.invitation_accepted_list.Datum;
import com.rohan.myvoice.pojo.invitation_accepted_list.InvitationList_accept_list;
import com.rohan.myvoice.pojo.invitation_details.Invite;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.rohan.myvoice.MainActivity.Build_alert_dialog;

/**
 * A simple {@link Fragment} subclass.
 */
public class InvitaionList extends Fragment {

    View v;
    ApiService api;
    String api_key;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private ProgressDialog progressDialog;
    private List<com.rohan.myvoice.pojo.invitation_accepted_list.Datum> invitation_list;
    private ListView invitation_list_view;
    Button pending_button, accepted_button;
    Button test_b;

    public InvitaionList() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_invitaion_list, container, false);
        progressDialog = new ProgressDialog(this.getActivity());
        progressDialog.setMax(100);
        progressDialog.setMessage("Loading");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        // mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipeToRefresh);
//        mSwipeRefreshLayout.setColorSchemeResources(R.color.dark_blue);

        pref = this.getActivity().getSharedPreferences("MYVOICEAPP_PREF", Context.MODE_PRIVATE);
        editor = pref.edit();

        api = RetroClient.getApiService();
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        TextView tt = toolbar.findViewById(R.id.title_text);
        tt.setText("Invitations");

        ImageView back = toolbar.findViewById(R.id.back_image);
        back.setVisibility(View.VISIBLE);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction().replace(R.id.framelayout_container, new NotificationFragment()).commit();
            }
        });

        api_key = getResources().getString(R.string.APIKEY);

        invitation_list = new ArrayList<>();

        invitation_list_view = v.findViewById(R.id.invitation_list);
        ListViewAdapter_of_pending_invitations listViewAdapter = new ListViewAdapter_of_pending_invitations(getActivity(), invitation_list);
        invitation_list_view.setAdapter(listViewAdapter);

        pending_button = v.findViewById(R.id.p_b);
        accepted_button = v.findViewById(R.id.a_b);

        // test_b = v.findViewById(R.id.test);

        pending_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("test", "pending button clicked");
                //change the buttons' background color
                //pending_button.setBackgroundColor(getResources().getColor(R.color.dark_blue));
                //accepted_button.setBackgroundColor(Color.TRANSPARENT);

                //change text colors too
                pending_button.setBackgroundDrawable(getResources().getDrawable(R.drawable.pending_button_style_active));
                accepted_button.setBackgroundDrawable(getResources().getDrawable(R.drawable.accepted_button_style_not_active));

                pending_button.setTextColor(Color.WHITE);
                accepted_button.setTextColor(getResources().getColor(R.color.dark_blue));

                //call for getting the list of pending invitations
                get_pending_list_call();


            }


        });

        accepted_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("test", "accepted button clicked");
                //change the buttons' background color
                //accepted_button.setBackgroundColor(getResources().getColor(R.color.dark_blue));
                //pending_button.setBackgroundColor(Color.TRANSPARENT);
                accepted_button.setBackgroundDrawable(getResources().getDrawable(R.drawable.accepted_button_style_active));
                pending_button.setBackgroundDrawable(getResources().getDrawable(R.drawable.pending_button_style_not_active));
                //change text colors too
                accepted_button.setTextColor(Color.WHITE);
                pending_button.setTextColor(getResources().getColor(R.color.dark_blue));


                //call for getting the list of accepted invitations

                get_accept_list_call();


            }
        });

        pending_button.callOnClick();




       /* test_b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("test", "test button is clicked");
            }
        });*/


    }

    private void get_accept_list_call() {
        invitation_list_view.setAdapter(null);


        Call<InvitationList_accept_list> call = api.getInvitaionList_accept_Json(api_key, "Token " + pref.getString("token", null));

        progressDialog.show();

        call.enqueue(new Callback<InvitationList_accept_list>() {
            @Override
            public void onResponse(Call<InvitationList_accept_list> call, Response<InvitationList_accept_list> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && "Success".equals(response.body().getStatus())) {

                    invitation_list = response.body().getData();
                    //if(invitation_list!=null || invitation_list.size() == 0){
                    ListViewAdapter_of_accepted_invitations adapter = new ListViewAdapter_of_accepted_invitations(getActivity(), invitation_list);
                    //invitation_list_view.setAdapter(null);
                   //adapter.notifyDataSetChanged();
                    invitation_list_view.setAdapter(adapter);

                    // }

                } else {
                    progressDialog.dismiss();

                    try {
                        JSONObject jObjError = new JSONObject(response.errorBody().string());
                        /* String status = jObjError.getString("detail");
                         */
                        if (jObjError.getString("detail").equals("Invalid Token")) {
                            update_token();
                            get_accept_list_call();
                        }
                    } catch (Exception e) {
                        //Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

            }

            @Override
            public void onFailure(Call<InvitationList_accept_list> call, Throwable t) {
                progressDialog.dismiss();
            }
        });
    }

    private void get_pending_list_call() {
        invitation_list_view.setAdapter(null);

        Call<InvitationList_accept_list> call = api.getInvitaionList_pending_Json(api_key, "Token " + pref.getString("token", null));

        progressDialog.show();

        call.enqueue(new Callback<InvitationList_accept_list>() {
            @Override
            public void onResponse(Call<InvitationList_accept_list> call, Response<InvitationList_accept_list> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && "Success".equals(response.body().getStatus())) {

                    invitation_list = response.body().getData();

                    //if(invitation_list!=null || invitation_list.size() == 0){

                    ListViewAdapter_of_pending_invitations adapter = new ListViewAdapter_of_pending_invitations(getActivity(), invitation_list);

                    invitation_list_view.setAdapter(adapter);
                    //}

                } else {
                    progressDialog.dismiss();

                    try {
                        JSONObject jObjError = new JSONObject(response.errorBody().string());
                        /* String status = jObjError.getString("detail");
                         */
                        if (jObjError.getString("detail").equals("Invalid Token")) {
                            update_token();
                            get_accept_list_call();
                        }
                    } catch (Exception e) {
                        //Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

            }

            @Override
            public void onFailure(Call<InvitationList_accept_list> call, Throwable t) {
                progressDialog.dismiss();
            }
        });
    }


    class ListViewAdapter_of_pending_invitations extends BaseAdapter {

        private List<com.rohan.myvoice.pojo.invitation_accepted_list.Datum> list;
        private Context context;

        public ListViewAdapter_of_pending_invitations(Context context, List<com.rohan.myvoice.pojo.invitation_accepted_list.Datum> list) {
            this.list = list;
            this.context = context;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.notification_list_view_item_1, null);
            TextView title_text = view.findViewById(R.id.title_text);
            TextView date = view.findViewById(R.id.date_text);
            ImageView iv = view.findViewById(R.id.image);

            title_text.setText(list.get(position).getTitle().toString().trim());
            date.setText(list.get(position).getDate().toString().trim());
            Glide.with(getActivity()).load(list.get(position).getLogo()).into(iv);

            Button accept_button = view.findViewById(R.id.accept_btn);
            Button ignore_button = view.findViewById(R.id.ignore_btn);

            accept_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    invitation_operation(list.get(position).getId().toString().trim(), "Accept");


                }


            });

            ignore_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    invitation_operation(list.get(position).getId().toString().trim(), "Ignore");

                }
            });

            return view;
        }

        private void invitation_operation(final String idOfInvitaiton, final String operation) {
            Call<Invite> call = api.getInviteJson(api_key, "Token " + pref.getString("token", null),
                    idOfInvitaiton, operation, "Android");
            progressDialog.show();
            call.enqueue(new Callback<Invite>() {
                @Override
                public void onResponse(Call<Invite> call, Response<Invite> response) {

                    if (response.isSuccessful() && "Success".equals(response.body().getStatus())) {
                        progressDialog.dismiss();
                        //redirect user to main survey li8st fragment
                        if ("Accept".equals(operation)) {
                            getFragmentManager().beginTransaction().replace(R.id.framelayout_container, new HomeFragment()).commit();
                        } else {
                            //refresh this fragment (i.e notifications fragment)
                            getFragmentManager().beginTransaction().replace(R.id.framelayout_container, new NotificationFragment()).commit();
                        }

                    } else {
                        progressDialog.dismiss();

                        try {
                            JSONObject jObjError = new JSONObject(response.errorBody().string());
                            /* String status = jObjError.getString("detail");
                             */
                            if (jObjError.getString("detail").equals("Invalid Token")) {
                                update_token();
                                invitation_operation(idOfInvitaiton, operation);
                            }
                        } catch (Exception e) {
                            //Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                        }

                    }
                }

                @Override
                public void onFailure(Call<Invite> call, Throwable t) {
                    progressDialog.dismiss();
                }
            });

        }
    }

    class ListViewAdapter_of_accepted_invitations extends BaseAdapter {

        private List<Datum> list;
        private Context context;

        public ListViewAdapter_of_accepted_invitations(Context context, List<com.rohan.myvoice.pojo.invitation_accepted_list.Datum> list) {
            this.list = list;
            this.context = context;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.invitation_accept_list_item, null);
            TextView title_text = view.findViewById(R.id.title_text);
            TextView date = view.findViewById(R.id.date_text);
            ImageView iv = view.findViewById(R.id.image);

            title_text.setText(list.get(position).getTitle().toString().trim());
            date.setText(list.get(position).getDate().toString().trim());
            Glide.with(getActivity()).load(list.get(position).getLogo()).into(iv);
            return view;
        }


    }


    public void update_token() {
        //pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //Toast.makeText(getActivity(), "email from pref: " + pref.getString("email", "not fatched from pref"), Toast.LENGTH_SHORT).show();
        ApiService api = RetroClient.getApiService();

        //if fcm token is null then do not write in shared pref!
        if (PublicClass.FCM_TOKEN != null) {
            editor.putString("fcm_token", PublicClass.FCM_TOKEN);
            editor.commit();
        }

        Call<Login> call = api.getLoginJason(pref.getString("email", null), pref.getString("password", null), pref.getString("fcm_token", null),
                "Android", Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID));
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
                        Build_alert_dialog(getActivity(), status, error_msg);
                    } catch (Exception e) {
                        //Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
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