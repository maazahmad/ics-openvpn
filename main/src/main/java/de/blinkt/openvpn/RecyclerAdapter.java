/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;


import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;


public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ServerHolder>{

    private ArrayList<String> mServers;
    //1
    public static class ServerHolder extends RecyclerView.ViewHolder  {
        //2
        private TextView mItemDate;
        private TextView mItemDescription;
        //3
        private static final String SERVER_KEY = "SERVER";
        //4
        public ServerHolder(View v) {
            super(v);
            mItemDate = (TextView) v.findViewById(R.id.item_date);
            mItemDescription = (TextView) v.findViewById(R.id.item_description);
        }
        public void bindServer(final String server) {
            mItemDate.setText(server.toString());
            mItemDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("log",server);
                }
            });
            Log.d("bindServer", server);
            mItemDescription.setText(server);
        }

    }

    @Override
    public RecyclerAdapter.ServerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View inflatedView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.itemserver, parent, false);
        return new ServerHolder(inflatedView);
    }

    @Override
    public void onBindViewHolder(RecyclerAdapter.ServerHolder holder, int position) {
        String itemServer = mServers.get(position);
        holder.bindServer(itemServer);
    }

    @Override
    public int getItemCount() {
        return mServers.size();
    }


    public RecyclerAdapter(ArrayList server) {

        mServers = ActivityDashboard.myServer;
        Log.d(" Run Already",mServers.toString());
    }
}
