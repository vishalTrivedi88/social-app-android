package com.rozdoum.socialcomponents.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.rozdoum.socialcomponents.R;
import com.rozdoum.socialcomponents.adapters.PostsAdapter;
import com.rozdoum.socialcomponents.enums.ProfileStatus;
import com.rozdoum.socialcomponents.managers.ProfileManager;
import com.rozdoum.socialcomponents.model.Post;
import com.rozdoum.socialcomponents.utils.GoogleApiHelper;
import com.rozdoum.socialcomponents.utils.LogUtil;
import com.rozdoum.socialcomponents.utils.PreferencesUtil;

public class MainActivity extends BaseActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();

    private PostsAdapter postsAdapter;
    private RecyclerView recyclerView;

    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    private ProfileManager profileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        profileManager = ProfileManager.getInstance(this);

        mAuth = FirebaseAuth.getInstance();
        initContentView();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.stopAutoManage(this);
            mGoogleApiClient.disconnect();
        }
    }

    private void initContentView() {
        if (recyclerView == null) {
            FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.addNewPostFab);

            if (floatingActionButton != null) {
                floatingActionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ProfileStatus profileStatus = profileManager.checkProfile();

                        if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
                            openCreatePostActivity();
                        } else {
                            doAuthorization(profileStatus);
                        }
                    }
                });
            }

            SwipeRefreshLayout swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
            recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
            postsAdapter = new PostsAdapter(this, swipeContainer);
            postsAdapter.setOnItemClickListener(new PostsAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(Post post) {
                    Intent intent = new Intent(MainActivity.this, PostDetailsActivity.class);
                    intent.putExtra(PostDetailsActivity.POST_ID_EXTRA_KEY, post.getId());
                    startActivity(intent);
                }
            });

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(postsAdapter);
            recyclerView.setAdapter(postsAdapter);
            postsAdapter.loadFirstPage();
        }
    }

    private void openCreatePostActivity() {
        Intent intent = new Intent(this, CreatePostActivity.class);
        startActivity(intent);
    }

    private void signOut() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                String providerId = profile.getProviderId();
                logoutByProvider(providerId);
            }
            logoutFirebase();
        }
    }

    private void logoutByProvider(String providerId) {
        switch (providerId) {
            case GoogleAuthProvider.PROVIDER_ID:
                logoutGoogle();
                break;

            case FacebookAuthProvider.PROVIDER_ID:
                logoutFacebook();
                break;
        }
    }

    private void logoutFirebase() {
        mAuth.signOut();
        PreferencesUtil.setProfileCreated(this, false);
    }

    private void logoutFacebook() {
        FacebookSdk.sdkInitialize(getApplicationContext());
        LoginManager.getInstance().logOut();
    }

    private void logoutGoogle() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = GoogleApiHelper.createGoogleApiClient(this);
        }

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }

        mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                if (mGoogleApiClient.isConnected()) {
                    Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                LogUtil.logDebug(TAG, "User Logged out from Google");
                            } else {
                                LogUtil.logDebug(TAG, "Error Logged out from Google");
                            }
                        }
                    });
                }
            }

            @Override
            public void onConnectionSuspended(int i) {
                LogUtil.logDebug(TAG, "Google API Client Connection Suspended");
            }
        });
    }

    private void openProfileActivity() {
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.profile:
                ProfileStatus profileStatus = profileManager.checkProfile();

                if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
                    openProfileActivity();
                } else {
                    doAuthorization(profileStatus);
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
