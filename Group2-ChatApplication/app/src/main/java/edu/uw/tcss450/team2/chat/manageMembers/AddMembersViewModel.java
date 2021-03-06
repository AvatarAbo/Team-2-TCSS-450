package edu.uw.tcss450.team2.chat.manageMembers;

import android.app.Application;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.uw.tcss450.team2.chat.createChatRoom.UserModel;
import edu.uw.tcss450.team2.databinding.FragmentManageMembersBinding;
import edu.uw.tcss450.team2.io.RequestQueueSingleton;
import edu.uw.tcss450.team2.model.UserInfoViewModel;


public class AddMembersViewModel extends AndroidViewModel {

    private MutableLiveData<List<UserModel>> mContacts;

    private UserInfoViewModel userInfoViewModel;
    private int chatId;
    private FragmentManageMembersBinding binding;

    private ManageMembersViewModel manageMembersViewModel;

    public AddMembersViewModel(@NonNull Application application) {
        super(application);

        mContacts = new MutableLiveData<>();
        mContacts.setValue(new ArrayList<>());

    }

    public void setManageMembersViewModel(ManageMembersViewModel manageMembersViewModel) {
        this.manageMembersViewModel = manageMembersViewModel;
    }

    public void setUserInfoViewModel(UserInfoViewModel userInfoViewModel) {
        this.userInfoViewModel = userInfoViewModel;
    }

    public void setChatId(int chatId) {
        this.chatId = chatId;
    }

    public void setBinding(FragmentManageMembersBinding binding) {
        this.binding = binding;
    }

    /*
     * method to send observer to alert the notification
     */

    public void addContactListObserver(@NonNull LifecycleOwner owner,
                                       @NonNull Observer<? super List<UserModel>> observer) {
        mContacts.observe(owner, observer);
    }

    /*
     * method to setup a contact edu.uw.tcss450.team2.friend from the web server
     *
     */

    public void getMembers(String jwt, int chatId, int memberId) throws JSONException {
        String url =
                "https://team-2-tcss-450-webservices.herokuapp.com/chatrooms/getContactsNotBelongChatRoom";

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("chatId", chatId);
        jsonBody.put("memberId", memberId);

        Request request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody, //no body for this get edu.uw.tcss450.team2.request
                this::handelSuccessForGetMembers,
                this::handleError) {
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    // add headers <key,value>
                    headers.put("Authorization", jwt);
                    return headers;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(
                10_000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        //Instantiate the RequestQueue and add the edu.uw.tcss450.team2.request to the queue
        RequestQueueSingleton.getInstance(getApplication().getApplicationContext())
                .addToRequestQueue(request);
    }


    public void addMember(String jwt, int chatId, int memberId) throws JSONException {
        binding.layoutWait.setVisibility(View.VISIBLE);
        String url =
                "https://team-2-tcss-450-webservices.herokuapp.com/chatrooms/addMemberToChatRoom";

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("chatId", chatId);
        jsonBody.put("memberId", memberId);

        Request request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody, //no body for this get edu.uw.tcss450.team2.request
                this::handelSuccessForAddMember,
                this::handleError) {
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                // add headers <key,value>
                headers.put("Authorization", jwt);
                return headers;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(
                10_000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        //Instantiate the RequestQueue and add the edu.uw.tcss450.team2.request to the queue
        RequestQueueSingleton.getInstance(getApplication().getApplicationContext())
                .addToRequestQueue(request);
    }


    private void handelSuccessForAddMember(final JSONObject response)  {
        try {
            getMembers(this.userInfoViewModel.getJwt(), chatId, userInfoViewModel.getMemberId());
            manageMembersViewModel.getMembers(userInfoViewModel.getJwt(), chatId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void handelSuccessForGetMembers(final JSONObject response)  {
        List<UserModel> list = new ArrayList<>();
        try {

            JSONArray messages = response.getJSONArray("rows");
            for(int i = 0; i < messages.length(); i++) {
                JSONObject message = messages.getJSONObject(i);

                UserModel friendContacts = new UserModel(
                        message.getInt("memberid"),
                        message.getString("firstname"),
                        message.getString("lastname"),
                        message.getString("username")
                );

                list.add(friendContacts);
            }

            mContacts.setValue(list);
            binding.layoutWait.setVisibility(View.GONE);
        }catch (JSONException e) {
            Log.e("JSON PARSE ERROR", "Found in handle Success FriendContactsViewModel");
            Log.e("JSON PARSE ERROR", "Error: " + e.getMessage());
        }
    }

    /*
    * helper method to handle a error from the edu.uw.tcss450.team2.request
    * @param: error
    */
    private void handleError(final VolleyError error) {
        if (Objects.isNull(error.networkResponse)) {
            Log.e("NETWORK ERROR", error.getMessage());
        }
        else {
            String data = new String(error.networkResponse.data, Charset.defaultCharset());
            Log.e("CLIENT ERROR",
                    error.networkResponse.statusCode +
                            " " +
                            data);
        }
    }
}
