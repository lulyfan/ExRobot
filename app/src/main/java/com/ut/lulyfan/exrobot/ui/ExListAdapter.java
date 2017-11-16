package com.ut.lulyfan.exrobot.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.model.Customer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/11/10/010.
 */

public class ExListAdapter extends RecyclerView.Adapter<ExListAdapter.MyViewHolder> {
    List<Customer> customers;
    Context context;
    int target;
    public static final int EX_LIST = 0;         //要派送的快递列表
    public static final int FAILED_EX_LIST = 1; //派送失败的快递列表

    //显示派送失败的快递列表
    public ExListAdapter(Context context, List<Customer> customers) {
        this.customers = customers;
        this.context = context;
        this.target = FAILED_EX_LIST;
    }

    //显示要派送的快递列表
    public ExListAdapter(Context context) {
        customers = new ArrayList<>();
        target = EX_LIST;
        this.context = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.exlist_item, null);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.tv_name.setText("姓名:" + customers.get(position).getName());
        holder.tv_phoneNum.setText("手机号:" + customers.get(position).getPhoneNum());
        holder.tv_count.setText(customers.get(position).getExCount() +"");

        if (target == EX_LIST) {
            holder.ib_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    customers.remove(position);
                    notifyDataSetChanged();
                }
            });
        } else if (target == FAILED_EX_LIST) {
            holder.ib_delete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return customers == null ? 0 : customers.size();
    }

    public List<Customer> getExList() {
        return customers;
    }

    public void addEx(Customer customer) {
        customers.add(customer);
        notifyDataSetChanged();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView tv_phoneNum;
        TextView tv_name;
        TextView tv_count;
        ImageButton ib_delete;

        public MyViewHolder(View itemView) {
            super(itemView);
            tv_phoneNum = (TextView) itemView.findViewById(R.id.phoneNum);
            tv_name = (TextView) itemView.findViewById(R.id.name);
            tv_count = (TextView) itemView.findViewById(R.id.exCount);
            ib_delete = (ImageButton) itemView.findViewById(R.id.delete);
        }
    }
}
