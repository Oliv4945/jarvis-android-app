package net.iopush.jarvis;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by oliv on 11/04/17.
 */

public class ConversationAdapter extends RecyclerView.Adapter<ConversationViewHolder> {

    List<ConversationObject> list;

    // Constructor, requires a list as parameter
    public ConversationAdapter(List<ConversationObject> list) {
        this.list = list;
    }

    // Creates viewHolders
    // and inflate view from XML
    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup viewGroup, int itemType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cell_conversation,viewGroup,false);
        return new ConversationViewHolder(view);
    }

    // Then fill the cell thanks to JarvisConversationObject
    @Override
    public void onBindViewHolder(ConversationViewHolder conversationViewHolder, int position) {
        ConversationObject myObject = list.get(position);
        conversationViewHolder.bind(myObject);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

}