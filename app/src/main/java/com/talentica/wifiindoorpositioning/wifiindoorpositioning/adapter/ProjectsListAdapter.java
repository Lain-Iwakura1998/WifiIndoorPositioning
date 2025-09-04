package com.talentica.wifiindoorpositioning.wifiindoorpositioning.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.talentica.wifiindoorpositioning.wifiindoorpositioning.R;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.model.IndoorProject;

import java.util.List;

/**
 * Created by suyashg on 25/08/17.
 */

public class ProjectsListAdapter extends RecyclerView.Adapter<ProjectsListAdapter.ViewHolder> {
    private List<IndoorProject> mDataset;

    // Provide a reference to the views for each data item
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name;

        public ViewHolder(LinearLayout v) {
            super(v);
            name = v.findViewById(R.id.tv_project_name);
        }
    }

    // Constructor now accepts a List instead of RealmResults
    public ProjectsListAdapter(List<IndoorProject> myDataset) {
        mDataset = myDataset;
    }

    @Override
    public ProjectsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_list, parent, false);
        return new ViewHolder(linearLayout);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.name.setText(mDataset.get(position).getName());
    }

    @Override
    public int getItemCount() {
        return mDataset != null ? mDataset.size() : 0;
    }
}
