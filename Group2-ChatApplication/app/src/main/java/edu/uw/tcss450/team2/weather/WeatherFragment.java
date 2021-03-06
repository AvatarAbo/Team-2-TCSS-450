package edu.uw.tcss450.team2.weather;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.uw.tcss450.team2.R;
import edu.uw.tcss450.team2.databinding.FragmentWeatherBinding;
import edu.uw.tcss450.team2.model.LocationViewModel;
import edu.uw.tcss450.team2.model.UserInfoViewModel;

/**
 * The main fragment of the weather tab. Displays the current, daily, and 'weekly' forecast, as well as the location whose weather is being displayed.
 *
 * @author Sam Spillers
 * @version 1.0
 */
public class WeatherFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    public static final float DISTANCE_TO_UPDATE_WEATHER = 1000;

    private Geocoder geocoder;

    private WeatherViewModel mWeatherModel;
    private FragmentWeatherBinding binding;
    private int currentPosition;
    private List<String> displayData;
    private ArrayList<MyLocation> myLocations;
    private ArrayAdapter<String> locationAdapter;
    private GPS myGPS;
    private UserInfoViewModel userInfoViewModel;

    private static final int PLACES_TO_ROUND_TO = 4;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    // A constant int for the permissions request code. Must be a 16 bit number
    private static final int MY_PERMISSIONS_LOCATIONS = 8414;
    private LocationRequest mLocationRequest;
    //Use a FusedLocationProviderClient to request the location
    private FusedLocationProviderClient mFusedLocationClient;
    // Will use this call back to decide what to do when a location change is detected
    private LocationCallback mLocationCallback;
    //The ViewModel that will store the current location
    private LocationViewModel mLocationModel;

    public WeatherFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentWeatherBinding.inflate(inflater);


        // Inflate the layout for this fragment
        return binding.getRoot();
    }

    /**
     * Populates the Spinner with data, asks for location permission if not already enabled.
     * @param view See inherited class
     * @param savedInstanceState See inherited class
     *
     * @author Sam Spillers
     * @version 1.0
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        geocoder = new Geocoder(getContext(), Locale.getDefault());
        myGPS = new GPS(geocoder);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_LOCATIONS);
        } else {
            //The user has already allowed the use of Locations. Get the current location.
            requestLocation();
        }

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    if (mLocationModel == null) {
                        mLocationModel = new ViewModelProvider(getActivity())
                                .get(LocationViewModel.class);
                    }
                    mLocationModel.setLocation(location);
                }
            };
        };

        userInfoViewModel = new ViewModelProvider(getActivity()).get(UserInfoViewModel.class);
        mLocationModel = new ViewModelProvider(getActivity()).get(LocationViewModel.class);
        mLocationModel.addLocationObserver(getViewLifecycleOwner(), this::handleNewLocation);

        createLocationRequest();

        currentPosition = 0;

        mWeatherModel = new ViewModelProvider(getActivity())
                .get(WeatherViewModel.class);
        mWeatherModel.addResponseObserver(
                getViewLifecycleOwner(),
                this::weatherUpdate);

        loadMyLocations();

        Spinner locationSpinner = binding.LocationSpinner;
        locationSpinner.setOnItemSelectedListener(this);

        locationAdapter = new ArrayAdapter<String>(getActivity(), R.layout.support_simple_spinner_dropdown_item, displayData);
        locationAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);

        FragmentWeatherBinding binding = FragmentWeatherBinding.bind(getView());

        updateWeather();

        binding.swipeRefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        updateWeather();
                    }
                }
        );

        if(getArguments() != null) {
            MyLocation newLocation = WeatherFragmentArgs.fromBundle(getArguments()).getNewMyLocationObject();
            if (newLocation != null) {
                addMyLocationObject(newLocation);
                selectMyLocationObject(newLocation);
            }
        }
    }

    /**
     * Saves the list of locations to shared preferences.
     *
     * @author Sam Spillers
     * @version 1.0
     */
    private void saveMyLocations() {
        // save the task list to preference
        SharedPreferences prefs = getActivity().getSharedPreferences(getResources().getString(R.string.weather_location_pref) + userInfoViewModel.getMemberId(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            editor.putString("TASKS", ObjectSerializer.serialize(new ArrayList<>(myLocations.subList(1, myLocations.size()))));
        } catch (IOException e) {
            e.printStackTrace();
        }
        editor.apply();
    }

    /**
     * Loads my locations from my shared preferences.
     *
     * @author Sam Spillers
     * @version 1.0
     */
    private void loadMyLocations() {
        if (myLocations != null) {
            myLocations.clear();

        }

        // load tasks from preference
        SharedPreferences prefs = getActivity().getSharedPreferences(getResources().getString(R.string.weather_location_pref) + userInfoViewModel.getMemberId(), Context.MODE_PRIVATE);

        try {
            myLocations = (ArrayList<MyLocation>) ObjectSerializer.deserialize(prefs.getString("TASKS", ObjectSerializer.serialize(new ArrayList<MyLocation>())));
            if (myLocations != null) {
                for (MyLocation l : myLocations) {
                    l.setGeocoder(geocoder);
                }
            }
            Log.e("myLocations", "myLocations: " + myLocations);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (myLocations == null) {
            myLocations = new ArrayList<>();
        }

        addMyLocationObject(myGPS, 0);
        updateSpinner();
        updateWeather();
    }

    /**
     * Selects the given location object in our locations list. Updates weather with this new item. Does nothing if location is not found.
     *
     * @param myLocation
     */
    private void selectMyLocationObject(MyLocation myLocation) {
        for (int index = 0; index < myLocations.size(); index++) {
            if (myLocations.get(index) == myLocation) {
                currentPosition = index;
                updateWeather();
                break;
            }
        }
    }

    /**
     * Adds a location to the end of our locations list.
     *
     * @param myLocation The location to add.
     * @author Sam Spillers
     * @version 1.0
     */
    private void addMyLocationObject(MyLocation myLocation) {
        addMyLocationObject(myLocation, myLocations.size());
    }

    /**
     * Adds a location to the specified location in our locations list.
     *
     * @param myLocation Location to add.
     * @param index Index at which to insert location. Implicitly throws exception if out of bounds index.
     * @author Sam Spillers
     * @version 1.0
     */
    private void addMyLocationObject(MyLocation myLocation, int index) {
        myLocations.add(index, myLocation);
        updateSpinner();
        saveMyLocations();
    }

    /**
     * Handles an updated location on the user's GPS. If GPS weather is currently being displayed and the new location is substantially different, the weather is updated.
     *
     * @param location The new GPS location received.
     * @author Sam Spillers
     * @version 1.0
     */
    private void handleNewLocation(Location location) {
        if (location != null) {
            if (myGPS.getLastLocation() != null) {
                if (location.distanceTo(myGPS.getLastLocation()) > DISTANCE_TO_UPDATE_WEATHER) {
                    myGPS.setLastLocation(location);
                    if (GPSSelected()) {
                        updateWeather();
                    }
                } else {
                    myGPS.setLastActualLocation(location);
                }
            } else {
                myGPS.setLastLocation(location);
                if (GPSSelected()) {
                    updateWeather();
                }
            }
        }
    }

    /**
     * Updates our spinner view to accurately reflect our current locations list.
     *
     * @author Sam Spillers
     * @version 1.0
     */
    private void updateSpinner() {
        if (displayData == null) {
            displayData = new ArrayList<>();
        }
        displayData.clear();
        for (MyLocation thing : myLocations) {
            displayData.add(thing.displayName());
        }
        displayData.add("Add new location...");
        if (locationAdapter != null) {
            locationAdapter.notifyDataSetChanged();
            binding.LocationSpinner.setSelection(currentPosition);
        }
    }

    /**
     * Checks whether the GPS's weather is currently being displayed.
     *
     * @return True if currently displaying weather based on the GPS's location. False otherwise.
     * @author Sam Spillers
     * @version 1.0
     */
    private boolean GPSSelected() {
        return myLocations.get(currentPosition) == myGPS;
    }

    /**
     * Refreshes the wqeather currently being displayed by sending a web request to our web server.
     *
     * @author Sam Spillers
     * @version 1.0
     */
    private void updateWeather() {
        if (!GPSSelected() || (GPSSelected() && myGPS.getLastActualLocation() != null)) {
            MyLocation location = myLocations.get(currentPosition);
            mWeatherModel.connect(location.getLocationString());
        }
    }

    /**
     * Handles a response from our web server with weather information contained.
     *
     * @param response The response from the webserver.
     */
    private void weatherUpdate(final JSONObject response) {
        binding.swipeRefresh.setRefreshing(false);
        updateSpinner();
        updateSpinner();
    }

    /**
     * Handles Spinner items being selected.
     *
     * @author Sam Spillers
     * @version 1.0
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (id == displayData.size() - 1) {  // If the last item (the choose new location item) was selected
            Navigation.findNavController(getView()).navigate(WeatherFragmentDirections.actionNavigationWeatherToChooseLocationFragment());
        } else {
            currentPosition = position;
            updateWeather();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_LOCATIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // locations-related task you need to do.
                    requestLocation();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("PERMISSION DENIED", "Nothing to see or do here.");
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /**
     * Requests the user's location. If permission is currently denied, then permission is asked for.
     *
     * @author Sam Spillers
     * @version 1.0
     */
    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("REQUEST LOCATION", "User did NOT allow permission to request location!");
        } else {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                if (mLocationModel == null) {
                                    mLocationModel = new ViewModelProvider(getActivity())
                                            .get(LocationViewModel.class);
                                }
                                mLocationModel.setLocation(location);
                            }
                        }
                    });
        }
    }

    /**
     * Sets up our Location Request object to request location data periodically.
     *
     * @author Sam Spillers
     * @version 1.0
     */
    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    // The following two pause and unpause our location requesting when the activity gains and loses focus.    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates();
    }
    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    /**
     * Begins requesting locations using our Location Request object.
     *
     * @author Sam Spillers
     * @version 1.0
     */
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);
        }
    }

    /**
     * Stops requesting locations using our Location Request object.
     *
     * @author Sam Spillers
     * @version 1.0
     */
    private void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * A helper function to round doubles to a given number of decimal places.
     *
     * @param value The double to round.
     * @param places The number of decimal places to round to.
     * @return The newly rounded double.
     * @author Sam Spillers
     * @version 1.0
     */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Represents a location that our weather can display.
     *
     * @author Sam Spillers
     * @version 1.0
     */
    public interface MyLocation extends Serializable {

        /**
         * Gets the location of the object in a String format interpretable by our web server.
         *
         * @return The parsable string representation of our location object.
         * @author Sam Spillers
         * @version 1.0
         */
        String getLocationString();

        /**
         * Gets the displayable string representation of the location being displayed.
         *
         * @return The displayable string representation of the location being displayed.
         * @author Sam Spillers
         * @version 1.0
         */
        String displayName();

        /**
         * Sets the geocoder this object is using. Used to set when the inital geocoder given in the constructor is null (Such as when say during reading from serialization).
         *
         * @param coder The geocoder for this object to use.
         * @author Sam Spillers
         * @version 1.0
         */
        void setGeocoder(Geocoder coder);
    }

    /**
     * Represents a weather location specified using coordinates.
     *
     * @author Sam Spillers
     * @version 1.0
     */
    public static class LongLat implements MyLocation {
        private double lat;
        private double lng;
        private transient Geocoder geocoder;  // Must be transient as geocoder is not serializiable so the geocoder cannot be saved.

        /**
         * Initializes a LongLat object using the given location's location.
         *
         * @param geocoder The geocoder for this object to use.
         * @param location The location to base this object's long and lat off of.
         * @author Sam Spillers
         * @version 1.0
         */
        public LongLat(Geocoder geocoder, Location location) {
            lat = location.getLatitude();
            lng = location.getLongitude();
            this.geocoder = geocoder;
        }
        @Override
        public String getLocationString() {
            return lat + "," + lng;
        }
        @Override
        public String displayName() {
            if (lat != 0 && lng != 0) {
                String coordinates = "(" + round(lat, PLACES_TO_ROUND_TO) + ", " + round(lng, PLACES_TO_ROUND_TO) + ")";
                try {
                    List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String name = address.getLocality();
                        String state = address.getAdminArea();
                        if (name != null) {
                            return name + ", " + state + " " + coordinates;
                        } else {
                            return "Coordinates " + coordinates;
                        }
                    } else {
                        return "Coordinates " + coordinates;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return "Coordinates " + coordinates;
                }
            } else {
                return "Coordinates";
            }
        }

        @Override
        public void setGeocoder(Geocoder coder) {
            geocoder = coder;
        }
    }

    /**
     * Represents a weather location specified by zip code.
     *
     * @author Sam Spillers
     * @version 1.0
     */
    public static class ZipCode implements MyLocation {
        private int mZipCode;
        private transient Geocoder geocoder;

        /**
         * Initializes a zip code object using the given zipcode.
         *
         * @param geocoder The geocoder to use.
         * @param zipCode The zipcode this object is based on.
         * @author Sam Spillers
         * @version 1.0
         */
        public ZipCode(Geocoder geocoder, int zipCode) {
            mZipCode = zipCode;
            this.geocoder = geocoder;
        }
        @Override
        public String getLocationString() {
            return String.valueOf(mZipCode);
        }
        @Override
        public String displayName() {
            String zipCode = "(" + mZipCode + ")";
            try {
                List<Address> addresses = geocoder.getFromLocationName(String.valueOf(mZipCode), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String name = address.getLocality();
                    String state = address.getAdminArea();
                    if (name != null) {
                        return name + ", " + state + " " + zipCode;
                    } else {
                        return "Zip Code " + zipCode;
                    }
                } else {
                    return "Zip Code " + zipCode;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Zip Code " + zipCode;
            }

        }

        @Override
        public void setGeocoder(Geocoder coder) {
            geocoder = coder;
        }
    }

    /**
     * Represents the weather location found using the user's GPS.
     *
     * @author Sam Spillers
     * @version 1.0
     */
    public static class GPS implements MyLocation {
        private Location lastLocation = null;
        private Location lastActualLocation = null;
        private transient Geocoder geocoder;

        /**
         * Initializes a GPS location object.
         *
         * @param geocoder Thge geocoder object to use
         * @author Sam Spillers
         * @version 1.0
         */
        public GPS(Geocoder geocoder) {
            this.geocoder = geocoder;
        }
        @Override
        public String getLocationString() {
            if (lastActualLocation != null) {
                return lastActualLocation.getLatitude() + "," + lastActualLocation.getLongitude();
            } else {
                return "London";
            }
        }
        @Override
        public String displayName() {
            if (lastLocation != null) {
                try {
                    List<Address> addresses = geocoder.getFromLocation(lastLocation.getLatitude(), lastLocation.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String name = address.getLocality();
                        String state = address.getAdminArea();
                        if (name != null) {
                            return name + ", " + state + " (Device location)";
                        } else {
                            return "Device location";
                        }
                    } else {
                        return "Device location";
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return "Device location";
                }
            } else {
                return "Device location";
            }
        }

        /**
         * Sets this GPS locations last displayed location.
         *
         * @param newLocation The new location received.
         * @author Sam Spillers
         * @version 1.0
         */
        private void setLastLocation(Location newLocation) {
            lastLocation = newLocation;
            setLastActualLocation(newLocation);
        }

        /**
         * Gets the last used location.
         *
         * @return The last used location
         * @author Sam Spillers
         * @version 1.0
         */
        private Location getLastLocation() {
            return lastLocation;
        }

        /**
         * Sets the last know location of the GPS.
         * @param newLocation The new known location.
         * @author Sam Spillers
         * @version 1.0
         */
        private void setLastActualLocation(Location newLocation) {
            lastActualLocation = newLocation;
        }

        /**
         * Gets the last known location of the GPS. May not be the same as the last displayed location (found using getLastLocation()).
         * @return
         */
        private Location getLastActualLocation() {
            return lastActualLocation;
        }

        @Override
        public void setGeocoder(Geocoder coder) {
            geocoder = coder;
        }
    }
}

// Wow this file got long.