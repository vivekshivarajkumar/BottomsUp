package bottomsup.mapbox.app.ui.search;

import androidx.lifecycle.ViewModelProviders;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.snackbar.Snackbar;
import com.uber.sdk.android.core.UberSdk;
import com.uber.sdk.android.rides.RideParameters;
import com.uber.sdk.android.rides.RideRequestButton;
import com.uber.sdk.android.rides.RideRequestButtonCallback;
import com.uber.sdk.rides.client.ServerTokenSession;
import com.uber.sdk.rides.client.SessionConfiguration;
import com.uber.sdk.rides.client.error.ApiError;

import bottomsup.mapbox.app.R;




public class SearchFragment extends Fragment {
    private static final String TAG = SearchFragment.class.getSimpleName();

    private SearchViewModel mViewModel;
    private RideRequestButton requestButton;

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.search_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(SearchViewModel.class);
        // TODO: Use the ViewModel
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



        SessionConfiguration config = new SessionConfiguration.Builder()
                // mandatory
                .setClientId("jApQmBcsKKYPrFJicc_rT7aJNB2ixzOs")
                // required for enhanced button features
//                .setServerToken("<TOKEN>")
                // required for implicit grant authentication
                //.setRedirectUri("<REDIRECT_URI>")
                // optional: set sandbox as operating environment
                .setEnvironment(SessionConfiguration.Environment.SANDBOX)
                .build();

        UberSdk.initialize(config);


        requestButton = view.findViewById(R.id.uberbutton);
        RideParameters rideParams = new RideParameters.Builder()
                // Optional product_id from /v1/products endpoint (e.g. UberX). If not provided, most cost-efficient product will be used
//                .setProductId("a1111c8c-c720-46c3-8534-2fcdd730040d")
                // Required for price estimates; lat (Double), lng (Double), nickname (String), formatted address (String) of dropoff location
                .setDropoffLocation(
                        12.9353852, 77.5336364, "College", "PES University")
                // Required for pickup estimates; lat (Double), lng (Double), nickname (String), formatted address (String) of pickup location
                .setPickupLocation(13.0419962, 77.5681443, "Home", "SubscriBook.com LLP")
                .build();
// set parameters for the RideRequestButton instance
        requestButton.setRideParameters(rideParams);
        ServerTokenSession session = new ServerTokenSession(config);
        requestButton.setSession(session);
        requestButton.setCallback(new RideRequestButtonCallback() {
            @Override
            public void onRideInformationLoaded() {

            }

            @Override
            public void onError(ApiError apiError) {


                Snackbar snackbar = Snackbar
                        .make(view, "Api error :  "+apiError.toString(), Snackbar.LENGTH_LONG);
                snackbar.show();

                Log.e(TAG, "onError: "+apiError);


            }

            @Override
            public void onError(Throwable throwable) {

                Log.e(TAG, "onError: "+throwable );

                Snackbar snackbar = Snackbar
                        .make(view, "Throwable", Snackbar.LENGTH_LONG);
                snackbar.show();


            }
        });
    }
}