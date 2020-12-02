package edu.uw.tcss450.team2.search;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SearchView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import edu.uw.tcss450.team2.R;
import edu.uw.tcss450.team2.databinding.FragmentSearchCardBinding;
import edu.uw.tcss450.team2.databinding.FragmentSearchContactsListBinding;
import edu.uw.tcss450.team2.friend.FriendContacts;
import edu.uw.tcss450.team2.friend.FriendContactsRecyclerViewAdapter;
import edu.uw.tcss450.team2.model.UserInfoViewModel;
import edu.uw.tcss450.team2.databinding.FragmentFriendListBinding;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SearchContactsListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SearchContactsListFragment extends Fragment {

    private SearchContactsViewModel mModel;
    private MutableLiveData<JSONObject> listMutableLiveData;
    private List<SearchContacts> mFriendContacts;
    private SearchContactsRecyclerViewAdapter searchContactsRecyclerViewAdapter;
    //    private AddFriendViewModel addFriendViewModel;
//    private FriendContacts friendContacts;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search_contacts_list, container, false);
    }

    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listMutableLiveData = new MutableLiveData<>();
        listMutableLiveData.setValue(new JSONObject());

        UserInfoViewModel userInfoViewModel = new ViewModelProvider(getActivity()).get(UserInfoViewModel.class);
        mModel = new ViewModelProvider(getActivity()).get(SearchContactsViewModel.class);
        mModel.getSearchContacts(userInfoViewModel.getJwt(), userInfoViewModel.getEmail());

    }

    @Override
    public void onViewCreated(@NonNull View view, @NonNull Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentSearchContactsListBinding binding = FragmentSearchContactsListBinding.bind(getView());

        mModel.addSearchContactsObserver(getViewLifecycleOwner(), searchContacts -> {

            searchContactsRecyclerViewAdapter = new SearchContactsRecyclerViewAdapter(searchContacts);

            if (!searchContacts.isEmpty()) {
                binding.recyclerView.setAdapter(searchContactsRecyclerViewAdapter);
                binding.recyclerViewWait.setVisibility(View.GONE);

                searchContactsRecyclerViewAdapter.setOnItemClickListener(new SearchContactsRecyclerViewAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(int position) {
                        String str = searchContacts.get(position).getmFirstname() + " " + searchContacts.get(position).getmLastname();
                        searchContacts.get(position).changedUsername(str);
                        searchContactsRecyclerViewAdapter.notifyItemChanged(position);
                    }

                    @Override
                    public void onAddClick(int position) {

                    }


                });

                binding.seachFriend.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        List<SearchContacts> filteredList = new ArrayList<>();
                        for (SearchContacts item : searchContacts) {
                            if (item.getmSearchUsername().toLowerCase().contains(s.toString().toLowerCase())) {
                                filteredList.add(item);
                            }
                        }
                        searchContactsRecyclerViewAdapter.filterList(filteredList);
                    }
                });

                binding.buttonCancelBtn.setOnClickListener(button -> {
                    binding.seachFriend.setText("");
                });



//                binding1.addButton.setOnClickListener(button -> {
//                    binding1.addButton.setText("sent request");
//                });
            }
        });

//        addFriendViewModel.addResponseObserver(getViewLifecycleOwner(), response ->
//                binding1.addButton.setOnClickListener( button -> {
//                    binding1.addButton.setText("Sent request");
//                }));

    }




}