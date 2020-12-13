package edu.uw.tcss450.team2.model;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class UserInfoViewModel extends ViewModel {

// Comented out by Hyeong Kim because of Merge conflict
//    private final String mEmail;
//    private final String jwt;
//    private final String firstName;
//    private final String lastName;

    private String mEmail;
    private String jwt;
    private int memberId;


    public UserInfoViewModel() {

    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

//    public UserInfoViewModel(String mEmail, String jwt, String firstName, String lastName) {
//        this.mEmail = mEmail;
//        this.jwt = jwt;
//        this.firstName = firstName;
//        this.lastName = lastName;
//    }
    public UserInfoViewModel(String mEmail, String jwt, int memberId) {
        this.mEmail = mEmail;
        this.jwt = jwt;
        this.memberId = memberId;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getJwt() {
        return jwt;
    }

//    public String getFirstName() {
//        return firstName;
//    }
//
//    public String getLastName() {
//        return lastName;
//    }

    public static class UserInfoViewModelFactory implements ViewModelProvider.Factory {
        private final String email;
        private final String jwt;
        private final int memberId;
//        private final String firstName;
//        private final String lastName;

//        public UserInfoViewModelFactory(String email, String jwt, String fName, String lName) {
//            this.email = email;
//            this.jwt = jwt;
//            this.firstName = fName;
//            this.lastName = lName;
//
//        }



        public UserInfoViewModelFactory(String email, String jwt, int memberId) {
            this.email = email;
            this.jwt = jwt;
            this.memberId = memberId;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass == edu.uw.tcss450.team2.model.UserInfoViewModel.class) {
                return (T) new edu.uw.tcss450.team2.model.UserInfoViewModel(email, jwt, memberId);
            }
            throw new IllegalArgumentException(
                    "Argument must be: " + edu.uw.tcss450.team2.model.UserInfoViewModel.class);
        }
    }

}
