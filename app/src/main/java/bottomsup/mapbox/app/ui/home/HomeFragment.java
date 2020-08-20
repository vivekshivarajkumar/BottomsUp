package bottomsup.mapbox.app.ui.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import bottomsup.mapbox.app.R;
import bottomsup.mapbox.app.adapter.LocationRecyclerViewAdapter;
import bottomsup.mapbox.app.model.IndividualLocation;
import bottomsup.mapbox.app.util.LinearLayoutManagerWithSmoothScroller;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfConversion;
import com.uber.sdk.android.core.UberSdk;
import com.uber.sdk.android.rides.RideParameters;
import com.uber.sdk.android.rides.RideRequestButton;
import com.uber.sdk.android.rides.RideRequestButtonCallback;
import com.uber.sdk.rides.client.ServerTokenSession;
import com.uber.sdk.rides.client.SessionConfiguration;
import com.uber.sdk.rides.client.error.ApiError;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static com.uber.sdk.android.core.utils.Preconditions.checkNotNull;
import static com.uber.sdk.android.core.utils.Preconditions.checkState;


public class HomeFragment extends Fragment implements
        LocationRecyclerViewAdapter.ClickListener, MapboxMap.OnMapClickListener  {

    private HomeViewModel homeViewModel;
    private static final LatLngBounds LOCKED_MAP_CAMERA_BOUNDS = new LatLngBounds.Builder()
            .include(new LatLng(40.87096725853152, -74.08277394720501))
            .include(new LatLng(40.67035340371385,
                    -73.87063900287112)).build();
    private static final LatLng MOCK_DEVICE_LOCATION_LAT_LNG = new LatLng(40.713469, -74.006735);
    private static final int MAPBOX_LOGO_OPACITY = 75;
    private static final int CAMERA_MOVEMENT_SPEED_IN_MILSECS = 1200;
    private static final float NAVIGATION_LINE_WIDTH = 9;
    private static final float BUILDING_EXTRUSION_OPACITY = .8f;
    private static final String PROPERTY_SELECTED = "selected";
    private static final String BUILDING_EXTRUSION_COLOR = "#c4dbed";
    private DirectionsRoute currentRoute;
    private FeatureCollection featureCollection;
    private MapboxMap mapboxMap;
    private MapView mapView;
    private RecyclerView locationsRecyclerView;
    private ArrayList<IndividualLocation> listOfIndividualLocations;
    private CustomThemeManager customThemeManager;
    private LocationRecyclerViewAdapter styleRvAdapter;
    private int chosenTheme;
    private String TAG = "HomeFragment";

    private Button getDirections;
    private Button stopRouteShow;
    private Button distanceButton;


    //Animator

    private LinearLayout playout;
    private ImageView menu;
    private ValueAnimator pwidthAnimator;
    private boolean isInflated=false;
    LinearLayout linearLayout;
    private Context ctx;
    private View bottomSheet;
    private RideRequestButton blackButton;
    private BottomSheetBehavior<View> mBottomSheetBehavior;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Log.e(TAG, "onCreateView: " );
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);

//        final TextView textView = root.findViewById(R.id.text_home);
//        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.e(TAG, "onViewCreated: ");




        bottomSheet = view.findViewById(R.id.bottom_sheet1);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setPeekHeight(120);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                }

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                }

                if (newState == BottomSheetBehavior.STATE_DRAGGING) {

                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

//        view.findViewById(R.id.menu).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_EXPANDED);
//            }
//        });


        // Create a GeoJSON feature collection from the GeoJSON file in the assets folder.
        try {
            getFeatureCollectionFromJson();
        } catch (Exception exception) {
            Log.e("MapActivity", "onCreate: " + exception);
            Toast.makeText(ctx, R.string.failure_to_load_file, Toast.LENGTH_LONG).show();
        }

        // Initialize a list of IndividualLocation objects for future use with recyclerview
        listOfIndividualLocations = new ArrayList<>();

        // Initialize the theme that was selected in the previous activity. The blue theme is set as the backup default.
        chosenTheme = R.style.AppTheme_Purple;

        // Set up the Mapbox map
        mapView = view.findViewById(R.id.mapView);
        locationsRecyclerView = view.findViewById(R.id.map_layout_rv);
        locationsRecyclerView.setVisibility(View.VISIBLE);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMapX) {

                // Initialize the custom class that handles marker icon creation and map styling based on the selected theme
                customThemeManager = new CustomThemeManager(chosenTheme, ctx);

//        mapboxMap.setStyle(new Style.Builder().fromUrl(customThemeManager.getMapStyle()), new Style.OnStyleLoaded() {
//          @Override
//          public void onStyleLoaded(@NonNull Style style) {

                mapboxMapX.setStyle(Style.LIGHT, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {

                        UiSettings uiSettings = mapboxMapX.getUiSettings();
                        uiSettings.setCompassEnabled(false);

                        // Setting the returned mapboxMap object (directly above) equal to the "globally declared" one
                         mapboxMap= mapboxMapX;

                        // Adjust the opacity of the Mapbox logo in the lower left hand corner of the map
//                        ImageView logo = mapView.findViewById(R.id.logoView);
//                        logo.setAlpha(MAPBOX_LOGO_OPACITY);

                        // Set bounds for the map camera so that the user can't pan the map outside of the NYC area
                        mapboxMap.setLatLngBoundsForCameraTarget(LOCKED_MAP_CAMERA_BOUNDS);

                        // Set up the SymbolLayer which will show the icons for each store location
                        initStoreLocationIconSymbolLayer();

                        // Set up the SymbolLayer which will show the selected store icon
                        initSelectedStoreSymbolLayer();

                        // Set up the LineLayer which will show the navigation route line to a particular store location
                        initNavigationPolylineLineLayer();

                        // Create a list of features from the feature collection
                        List<Feature> featureList = featureCollection.features();

                        // Retrieve and update the source designated for showing the store location icons
                        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("store-location-source-id");
                        if (source != null) {
                            source.setGeoJson(FeatureCollection.fromFeatures(featureList));
                        }

                        if (featureList != null) {

                            for (int x = 0; x < featureList.size(); x++) {

                                Feature singleLocation = featureList.get(x);

                                // Get the single location's String properties to place in its map marker
                                String singleLocationName = singleLocation.getStringProperty("name");
                                String singleLocationHours = singleLocation.getStringProperty("hours");
                                String singleLocationDescription = singleLocation.getStringProperty("description");
                                String singleLocationPhoneNum = singleLocation.getStringProperty("phone");


                                // Add a boolean property to use for adjusting the icon of the selected store location
                                singleLocation.addBooleanProperty(PROPERTY_SELECTED, false);

                                // Get the single location's LatLng coordinates
                                Point singleLocationPosition = (Point) singleLocation.geometry();

                                // Create a new LatLng object with the Position object created above
                                LatLng singleLocationLatLng = new LatLng(singleLocationPosition.latitude(),
                                        singleLocationPosition.longitude());

                                // Add the location to the Arraylist of locations for later use in the recyclerview
                                listOfIndividualLocations.add(new IndividualLocation(
                                        singleLocationName,
                                        singleLocationDescription,
                                        singleLocationHours,
                                        singleLocationPhoneNum,
                                        singleLocationLatLng
                                ));

                                // Call getInformationFromDirectionsApi() to eventually display the location's
                                // distance from mocked device location
                                getInformationFromDirectionsApi(singleLocationPosition, false, x);

                            }
                            // Add the fake device location marker to the map. In a real use case scenario,
                            // the Maps SDK's LocationComponent can be used to easily display and customize
                            // the device location's puck
                            addMockDeviceLocationMarkerToMap();

                            setUpRecyclerViewOfLocationCards(chosenTheme);

                            mapboxMap.addOnMapClickListener(HomeFragment.this);

                            // TODO  Show 3d buildings if the blue theme is being used
//              if (customThemeManager.getNavigationLineColor() == R.color.colorPrimary_yellow) {
//                showBuildingExtrusions();
//              }

                        }
                    }

                });

            }
        });

        getDirections = view.findViewById(R.id.get_directions);
        distanceButton = view.findViewById(R.id.distance_to_you);
        stopRouteShow = view.findViewById(R.id.stop_show_route);
        getDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                startPopulatingViews();
            }
        });


    }

    private void startPopulatingViews() {
        locationsRecyclerView.setVisibility(View.GONE);
        Animation animZoomIn = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.zoom_anim);
        Animation rotation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotation_anim);
        stopRouteShow.setVisibility(View.VISIBLE);
        stopRouteShow.setAnimation(animZoomIn);
        distanceButton.setVisibility(View.VISIBLE);
        distanceButton.setAnimation(rotation);
    }


    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        handleClickIcon(mapboxMap.getProjection().toScreenLocation(point));
        return true;
    }

    private boolean handleClickIcon(PointF screenPoint) {

        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, "store-location-layer-id");
        if (!features.isEmpty()) {
            String name = features.get(0).getStringProperty("name");
            List<Feature> featureList = featureCollection.features();
            for (int i = 0; i < featureList.size(); i++) {

                if (featureList.get(i).getStringProperty("name").equals(name)) {
                    Point selectedFeaturePoint = (Point) featureList.get(i).geometry();

                    if (featureSelectStatus(i)) {
                        setFeatureSelectState(featureList.get(i), false);
                        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    } else {
                        setSelected(i);
                        //TODO here!
                        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
//                    if (selectedFeaturePoint.latitude() != MOCK_DEVICE_LOCATION_LAT_LNG.getLatitude()) {
//                        for (int x = 0; x < featureCollection.features().size(); x++) {
//
//                            if (listOfIndividualLocations.get(x).getLocation().getLatitude() == selectedFeaturePoint.latitude()) {
//                                // Scroll the recyclerview to the selected marker's card. It's "x-1" below because
//                                // the mock device location marker is part of the marker list but doesn't have its own card
//                                // in the actual recyclerview.
//                                locationsRecyclerView.smoothScrollToPosition(x);
//                            }
//                        }
//                    }
//                    // Check for an internet connection before making the call to Mapbox Directions API
//                    if (deviceHasInternetConnection()) {
//                        // Start call to the Mapbox Directions API
//                        getInformationFromDirectionsApi(selectedFeaturePoint, true, null);
//                    } else {
//                        Toast.makeText(ctx, R.string.no_internet_message, Toast.LENGTH_LONG).show();
//                    }
                } else {
                    setFeatureSelectState(featureList.get(i), false);
                }
            }
            return true;
        } else {
            return false;
        }

    }

    /**
     * The LocationRecyclerViewAdapter's interface which listens to clicks on each location's card
     *
     * @param position the clicked card's position/index in the overall list of cards
     */
    @Override
    public void onItemClick(int position) {
        // Get the selected individual location via its card's position in the recyclerview of cards
        IndividualLocation selectedLocation = listOfIndividualLocations.get(position);

        // Evaluate each Feature's "select state" to appropriately style the location's icon
        List<Feature> featureList = featureCollection.features();
        Point selectedLocationPoint = (Point) featureCollection.features().get(position).geometry();
        for (int i = 0; i < featureList.size(); i++) {
            if (featureList.get(i).getStringProperty("name").equals(selectedLocation.getName())) {
                if (featureSelectStatus(i)) {
                    setFeatureSelectState(featureList.get(i), false);
                } else {
                    setSelected(i);
                }
            } else {
                setFeatureSelectState(featureList.get(i), false);
            }
        }

        // Reposition the map camera target to the selected marker
        if (selectedLocation != null) {
            repositionMapCamera(selectedLocationPoint);
        }

        // Check for an internet connection before making the call to Mapbox Directions API
        if (deviceHasInternetConnection()) {
            // Start call to the Mapbox Directions API
            if (selectedLocation != null) {
                getInformationFromDirectionsApi(selectedLocationPoint, true, null);
            }
        } else {
            Toast.makeText(ctx, R.string.no_internet_message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Adds a SymbolLayer which will show all of the location's icons
     */
    private void initStoreLocationIconSymbolLayer() {
        Style style = mapboxMap.getStyle();
        if (style != null) {
            // Add the icon image to the map
            style.addImage("store-location-icon-id", customThemeManager.getSelectedMarkerIcon());

            // Create and add the GeoJsonSource to the map
            GeoJsonSource storeLocationGeoJsonSource = new GeoJsonSource("store-location-source-id");
            style.addSource(storeLocationGeoJsonSource);

            // Create and add the store location icon SymbolLayer to the map
            SymbolLayer storeLocationSymbolLayer = new SymbolLayer("store-location-layer-id",
                    "store-location-source-id");
            storeLocationSymbolLayer.withProperties(
                    iconImage("store-location-icon-id"),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                    iconSize(0.2f)
            );
            style.addLayer(storeLocationSymbolLayer);

        } else {
            Log.d("StoreFinderActivity", "initStoreLocationIconSymbolLayer: Style isn't ready yet.");

            throw new IllegalStateException("Style isn't ready yet.");
        }
    }

    /**
     * Adds a SymbolLayer which will show the selected location's icon
     */
    private void initSelectedStoreSymbolLayer() {
        Style style = mapboxMap.getStyle();
        if (style != null) {

            // Add the icon image to the map
            style.addImage("selected-store-location-icon-id", customThemeManager.getSelectedMarkerIcon());
            // Create and add the store location icon SymbolLayer to the map
            SymbolLayer selectedStoreLocationSymbolLayer = new SymbolLayer("selected-store-location-layer-id",
                    "store-location-source-id");
            selectedStoreLocationSymbolLayer.withProperties(
                    iconImage("selected-store-location-icon-id"),
                    iconAllowOverlap(true),
                    iconSize(0.3f)
            );
            selectedStoreLocationSymbolLayer.withFilter(eq((get(PROPERTY_SELECTED)), literal(true)));
            style.addLayer(selectedStoreLocationSymbolLayer);
        } else {
            Log.d("StoreFinderActivity", "initSelectedStoreSymbolLayer: Style isn't ready yet.");
            throw new IllegalStateException("Style isn't ready yet.");
        }
    }

    /**
     * Checks whether a Feature's boolean "selected" property is true or false
     *
     * @param index the specific Feature's index position in the FeatureCollection's list of Features.
     * @return true if "selected" is true. False if the boolean property is false.
     */
    private boolean featureSelectStatus(int index) {
        if (featureCollection == null) {
            return false;
        }
        return featureCollection.features().get(index).getBooleanProperty(PROPERTY_SELECTED);
    }

    /**
     * Set a feature selected state.
     *
     * @param index the index of selected feature
     */
    private void setSelected(int index) {
        Feature feature = featureCollection.features().get(index);
        setFeatureSelectState(feature, true);
        refreshSource();
    }

    /**
     * Selects the state of a feature
     *
     * @param feature the feature to be selected.
     */
    private void setFeatureSelectState(Feature feature, boolean selectedState) {
        feature.properties().addProperty(PROPERTY_SELECTED, selectedState);
        refreshSource();
    }


    /**
     * Updates the display of data on the map after the FeatureCollection has been modified
     */
    private void refreshSource() {
        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("store-location-source-id");
        if (source != null && featureCollection != null) {
            source.setGeoJson(featureCollection);
        }
    }

    private void getInformationFromDirectionsApi(Point destinationPoint,
                                                 final boolean fromMarkerClick, @Nullable final Integer listIndex) {
        // Set up origin and destination coordinates for the call to the Mapbox Directions API
        Point mockCurrentLocation = Point.fromLngLat(MOCK_DEVICE_LOCATION_LAT_LNG.getLongitude(),
                MOCK_DEVICE_LOCATION_LAT_LNG.getLatitude());

        Point destinationMarker = Point.fromLngLat(destinationPoint.longitude(), destinationPoint.latitude());

        // Initialize the directionsApiClient object for eventually drawing a navigation route on the map
        MapboxDirections directionsApiClient = MapboxDirections.builder()
                .origin(mockCurrentLocation)
                .destination(destinationMarker)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .accessToken("pk.eyJ1IjoiY2hldGFubnlhbWFnb3VkMTA4IiwiYSI6ImNrOHp6MmJhbDBqNnMzZGxjbjhha2F1cHIifQ.9v_TtcBp-yVPvdBQWKnIHg")
                .build();

        directionsApiClient.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                // Check that the response isn't null and that the response has a route
                if (response.body() == null) {
                    Log.e("MapActivity", "No routes found, make sure you set the right user and access token.");
                } else if (response.body().routes().size() < 1) {
                    Log.e("MapActivity", "No routes found");
                } else {
                    if (fromMarkerClick) {
                        // Retrieve and draw the navigation route on the map
                        currentRoute = response.body().routes().get(0);
                        drawNavigationPolylineRoute(currentRoute);

                    } else {
                        // Use Mapbox Turf helper method to convert meters to miles and then format the mileage number
                        DecimalFormat df = new DecimalFormat("#.#");
                        String finalConvertedFormattedDistance = String.valueOf(df.format(TurfConversion.convertLength(
                                response.body().routes().get(0).distance(), TurfConstants.UNIT_METERS,
                                TurfConstants.UNIT_KILOMETERS)));

                        // Set the distance for each location object in the list of locations
                        if (listIndex != null) {
                            listOfIndividualLocations.get(listIndex).setDistance(finalConvertedFormattedDistance);
                            // Refresh the displayed recyclerview when the location's distance is set
                            styleRvAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Toast.makeText(ctx, R.string.failure_to_retrieve, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void repositionMapCamera(Point newTarget) {
        CameraPosition newCameraPosition = new CameraPosition.Builder()
                .target(new LatLng(newTarget.latitude(), newTarget.longitude()))
                .build();
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition), CAMERA_MOVEMENT_SPEED_IN_MILSECS);
    }

    private void addMockDeviceLocationMarkerToMap() {
        // Add the fake user location marker to the map
        Style style = mapboxMap.getStyle();
        if (style != null) {
            // Add the icon image to the map
            style.addImage("mock-device-location-icon-id", customThemeManager.getMockLocationIcon());

            style.addSource(new GeoJsonSource("mock-device-location-source-id", Feature.fromGeometry(
                    Point.fromLngLat(MOCK_DEVICE_LOCATION_LAT_LNG.getLongitude(), MOCK_DEVICE_LOCATION_LAT_LNG.getLatitude()))));

            style.addLayer(new SymbolLayer("mock-device-location-layer-id",
                    "mock-device-location-source-id").withProperties(
                    iconImage("mock-device-location-icon-id"),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true)
            ));
        } else {
            throw new IllegalStateException("Style isn't ready yet.");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void getFeatureCollectionFromJson() throws IOException {
        try {
            // Use fromJson() method to convert the GeoJSON file into a usable FeatureCollection object
            featureCollection = FeatureCollection.fromJson(loadGeoJsonFromAsset("list_of_locations.geojson"));

        } catch (Exception exception) {
            Log.e("MapActivity", "getFeatureCollectionFromJson: " + exception);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String loadGeoJsonFromAsset(String filename) {
        try {
            // Load the GeoJSON file from the local asset folder
            InputStream is = ctx.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            Log.e("MapActivity", "Exception Loading GeoJSON: " + exception.toString());
            exception.printStackTrace();
            return null;
        }
    }

    private void setUpRecyclerViewOfLocationCards(int chosenTheme) {
        // Initialize the recyclerview of location cards and a custom class for automatic card scrolling
        locationsRecyclerView.setHasFixedSize(true);
        locationsRecyclerView.setLayoutManager(new LinearLayoutManagerWithSmoothScroller(ctx));
        styleRvAdapter = new LocationRecyclerViewAdapter(listOfIndividualLocations,
                getApplicationContext(), this, chosenTheme);
        locationsRecyclerView.setAdapter(styleRvAdapter);
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(locationsRecyclerView);
    }

    private void drawNavigationPolylineRoute(DirectionsRoute route) {
        // Retrieve and update the source designated for showing the store location icons
        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("navigation-route-source-id");
        if (source != null) {
            source.setGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(
                    LineString.fromPolyline(route.geometry(), PRECISION_6))));
        }
    }

    private void initNavigationPolylineLineLayer() {
        // Create and add the GeoJsonSource to the map
        GeoJsonSource navigationLineLayerGeoJsonSource = new GeoJsonSource("navigation-route-source-id");
        mapboxMap.getStyle().addSource(navigationLineLayerGeoJsonSource);

        // Create and add the LineLayer to the map to show the navigation route line
        LineLayer navigationRouteLineLayer = new LineLayer("navigation-route-layer-id",
                navigationLineLayerGeoJsonSource.getId());
        navigationRouteLineLayer.withProperties(
                lineColor(customThemeManager.getNavigationLineColor()),
                lineWidth(NAVIGATION_LINE_WIDTH)
        );
        mapboxMap.getStyle().addLayerBelow(navigationRouteLineLayer, "store-location-layer-id");
    }

    private boolean deviceHasInternetConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getApplicationContext().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ctx=context;
        Log.e(TAG, "onAttach: " );
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.e(TAG, "onDetach: " );
    }


    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
        Log.e(TAG, "onStart: " );
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        Log.e(TAG, "onStop: " );
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        Log.e(TAG, "onPause: " );
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        Log.e(TAG, "onResume: " );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        Log.e(TAG, "onDestroy: " );
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e(TAG, "onDestroyView: " );
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();

    }

    class CustomThemeManager {
        private int selectedTheme;
        private Context context;
        private Bitmap unselectedMarkerIcon;
        private Bitmap selectedMarkerIcon;
        private Bitmap mockLocationIcon;
        private int navigationLineColor;
        private String mapStyle;

        CustomThemeManager(int selectedTheme, Context context) {
            this.selectedTheme = selectedTheme;
            this.context = context;
            initializeTheme();
        }

        private void initializeTheme() {
            switch (selectedTheme) {
                case R.style.AppTheme_Blue:
                    mapStyle = getString(R.string.blue_map_style);
                    navigationLineColor = getResources().getColor(R.color.navigationRouteLine_blue);
                    unselectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.picon);
                    selectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.picon);
                    mockLocationIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.blue_user_location);
                    break;
                case R.style.AppTheme_Purple:
                    mapStyle = getString(R.string.purple_map_style);
                    navigationLineColor = getResources().getColor(R.color.colorPrimary_yellow);
                    unselectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.picon);
                    selectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.picon);
                    mockLocationIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.purple_user_location);
                    break;

//        case R.style.AppTheme_Purple:
//          mapStyle = getString(R.string.purple_map_style);
//          navigationLineColor = getResources().getColor(R.color.navigationRouteLine_purple);
//          unselectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.lopop);
//          selectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.lopop);
//          mockLocationIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.lopop);
//          break;

                case R.style.AppTheme_Green:
                    mapStyle = getString(R.string.terminal_map_style);
                    navigationLineColor = getResources().getColor(R.color.navigationRouteLine_green);
                    unselectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.green_unselected_money);
                    selectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.green_selected_money);
                    mockLocationIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.green_user_location);
                    break;
                case R.style.AppTheme_Neutral:
                    mapStyle = Style.MAPBOX_STREETS;
                    navigationLineColor = getResources().getColor(R.color.navigationRouteLine_neutral);
                    unselectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.white_unselected_house);
                    selectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.gray_selected_house);
                    mockLocationIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.neutral_orange_user_location);
                    break;
                case R.style.AppTheme_Gray:
                    mapStyle = Style.LIGHT;
                    navigationLineColor = getResources().getColor(R.color.navigationRouteLine_gray);
                    unselectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.white_unselected_bike);
                    selectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.gray_selected_bike);
                    mockLocationIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.gray_user_location);
                    break;
            }
        }

        public Bitmap getUnselectedMarkerIcon() {
            return unselectedMarkerIcon;
        }

        public Bitmap getMockLocationIcon() {
            return mockLocationIcon;
        }

        public Bitmap getSelectedMarkerIcon() {
            return selectedMarkerIcon;
        }

        int getNavigationLineColor() {
            return navigationLineColor;
        }

        public String getMapStyle() {
            return mapStyle;
        }
    }
}