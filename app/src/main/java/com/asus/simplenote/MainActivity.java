package com.asus.simplenote;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.asus.simplenote.databinding.ActivityMainBinding;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int RC_LOGIN = 0x010;
    final String DELETE_NOTE_TAG = "confirm-deletion";
    final String NOTE_TAG = "NOTE";


    private ActivityMainBinding ui;
    private FirebaseNoteAdapter mNoteAdapter;

    private String mUserID;
    private FirebaseAuth mFireAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFireDatabase;
    private DatabaseReference mFireNotesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_main);

        setSupportActionBar(ui.toolbar);


        mFireDatabase = FirebaseUtils.getDatabase();
        mFireAuth = FirebaseAuth.getInstance();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {

                    String userID = user.getUid();
                    onSignInInit(userID);
                }
                else {

                    onSignOutCleanUp();
                    startSignInActivity();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFireAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFireAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mNoteAdapter != null) mNoteAdapter.cleanup();
    }

    private void onSignOutCleanUp() {
        if(mNoteAdapter != null) mNoteAdapter.cleanup();
    }

    private void onSignInInit(String userID) {
        mUserID = userID;
        String userPath = "users/" + userID + "/notes";
        mFireNotesRef = mFireDatabase.getReference().child(userPath);

        mNoteAdapter = new FirebaseNoteAdapter(this, mUserID, mFireNotesRef);
        mNoteAdapter.setOnDataChangedListener(new FirebaseNoteAdapter.OnDataChangedListener() {
            @Override
            public void OnDataChanged() {
                ui.progressBar.setVisibility(View.GONE);
                int visibility = mNoteAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE;
                findViewById(R.id.no_notes_layout).setVisibility(visibility);
            }
        });

        mNoteAdapter.setOnNoteDeleteListener(new FirebaseNoteAdapter.OnDeleteNoteListener() {
            @Override
            public void OnNoteDelete(Note note, String userId) {
                ConfirmationDialog dialog = new ConfirmationDialog()
                        .setTitle(R.string.note_deletion_dialog_title)
                        .setMessage(R.string.action_warning)
                        .setOnYesClickListener(onYesDeleteNoteListener);
                dialog.mDialogState.extras.putSerializable(NOTE_TAG, note);
                dialog.show(getSupportFragmentManager(), DELETE_NOTE_TAG);
            }
        });


        ui.content.noteContainer.setItemAnimator(new DefaultItemAnimator());
        ui.content.noteContainer.setLayoutManager(new LinearLayoutManager(this));
        ui.content.noteContainer.setAdapter(mNoteAdapter);

        ui.newNoteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NoteEditorActivity.startActivity(MainActivity.this, mUserID);
            }
        });

        ConfirmationDialog.resetOnYesClickListener(MainActivity.this, DELETE_NOTE_TAG, onYesDeleteNoteListener);
    }

    private void startSignInActivity() {
        List<AuthUI.IdpConfig> authProviders = Arrays.asList(
                new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build()

        );

        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setTheme(R.style.AppThemeFirebase)
                        .setLogo(R.mipmap.ic_launcher_round)
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(authProviders)
                        .build(),
                RC_LOGIN
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == RC_LOGIN && resultCode != RESULT_OK) finish();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {

            AuthUI.getInstance().signOut(this);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    ConfirmationDialog.MyClickListener onYesDeleteNoteListener = new ConfirmationDialog.MyClickListener() {
        @Override
        public void onClick(Bundle extras) {
            Note note = (Note) extras.getSerializable(NOTE_TAG);
            if (note != null) {
                mFireNotesRef.child(note.getPushId())
                        .removeValue()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(MainActivity.this, R.string.deletion_success, Toast.LENGTH_SHORT).show();
                            }
                        });
            }

        }
    };


}
