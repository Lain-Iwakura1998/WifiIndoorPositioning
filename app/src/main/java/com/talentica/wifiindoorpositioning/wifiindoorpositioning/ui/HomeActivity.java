package com.talentica.wifiindoorpositioning.wifiindoorpositioning.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.talentica.wifiindoorpositioning.wifiindoorpositioning.R;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.adapter.ProjectsListAdapter;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.model.IndoorProject;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.utils.RecyclerItemClickListener;

import java.util.List;

public class HomeActivity extends AppCompatActivity
        implements View.OnClickListener, RecyclerItemClickListener.OnItemClickListener {

    // 直接引用 NewProjectActivity 的静态列表
    private List<IndoorProject> projects = NewProjectActivity.projectList;
    private RecyclerView mRecyclerView;
    private ProjectsListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();

        if (projects.isEmpty()) {
            Snackbar.make(fab, "Empty List, Try creating project", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }

        mAdapter = new ProjectsListAdapter(projects);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次返回界面时刷新列表
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            Intent intent = new Intent(this, UnifiedNavigationActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initUI() {
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);

        mRecyclerView = findViewById(R.id.projects_recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        );
        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, mRecyclerView, this)
        );
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == fab.getId()) {
            Intent intent = new Intent(this, NewProjectActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        if (position >= 0 && position < projects.size()) {
            Intent intent = new Intent(this, ProjectDetailActivity.class);
            IndoorProject project = projects.get(position);
            intent.putExtra("id", project.getId());
            startActivity(intent);
        }
    }

    @Override
    public void onLongClick(View view, int position) {
        // 暂不处理长按
    }
}
