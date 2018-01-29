package com.asus.simplenote;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.Query;


public class FirebaseNoteAdapter extends FirebaseRecyclerAdapter<Note, FirebaseNoteAdapter.NoteViewHolder> {


    private AppCompatActivity mContext;
    private String mUserID;

    interface OnDeleteNoteListener {
        void OnNoteDelete(Note note, String userId);
    }
    private OnDeleteNoteListener mOnNoteDeleteClickedListener;

    interface OnDataChangedListener {
        void OnDataChanged();
    }
    private OnDataChangedListener mOnDataChangedListener;

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        public Toolbar toolbar;
        public TextView text;
        public LinearLayout noteContent;

        public NoteViewHolder(View itemView) {
            super(itemView);

            toolbar = (Toolbar) itemView.findViewById(R.id.note_toolbar);
            text = (TextView) itemView.findViewById(R.id.note_text);
            noteContent = (LinearLayout) itemView.findViewById(R.id.note_content);
        }
    }

    public FirebaseNoteAdapter(AppCompatActivity context, String firebaseUserId, Query query){
        super(Note.class, R.layout.note_view_layout, FirebaseNoteAdapter.NoteViewHolder.class, query);
        this.mContext = context;
        this.mUserID = firebaseUserId;
    }

    @Override
    protected void populateViewHolder(FirebaseNoteAdapter.NoteViewHolder holder, final Note note, int position) {
        note.setPushId(getRef(position).getKey());
        holder.text.setText(note.getText());
        holder.toolbar.setTitle(note.getName());

        int menuSize = holder.toolbar.getMenu().size();
        if (menuSize == 0) {
            holder.toolbar.inflateMenu(R.menu.note_view_menu);
        }


        holder.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                switch (id){
                    case(R.id.action_delete):{
                        if(mOnNoteDeleteClickedListener != null) {
                            mOnNoteDeleteClickedListener.OnNoteDelete(note, mUserID);
                        }

                    }break;
                }
                return false;
            }
        });

        holder.noteContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NoteEditorActivity.startActivity(mContext, mUserID, note);
            }
        });
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        mOnDataChangedListener.OnDataChanged();
    }

    public void setOnDataChangedListener(OnDataChangedListener listener){
        mOnDataChangedListener = listener;
    }

    public void setOnNoteDeleteListener(OnDeleteNoteListener listener){
        mOnNoteDeleteClickedListener = listener;
    }


}
