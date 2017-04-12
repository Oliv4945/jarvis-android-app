package net.iopush.jarvis;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

// From http://tutos-android-france.com/material-design-recyclerview-et-cardview/
public class ConversationViewHolder extends RecyclerView.ViewHolder{

    private TextView textViewName;
    private TextView textViewText;

    // itemView is the view corresponding to one cell
    public ConversationViewHolder(View itemView) {
        super(itemView);

        // Match views
        textViewName = (TextView) itemView.findViewById(R.id.textViewName);
        textViewText = (TextView) itemView.findViewById(R.id.textViewText);
    }

    // Then add a function to fill the cell thanks to JarvisConversationObject
    public void bind(ConversationObject jarvisConversationObject){
        textViewName.setText(jarvisConversationObject.getName());
        textViewText.setText(jarvisConversationObject.getText());
    }
}