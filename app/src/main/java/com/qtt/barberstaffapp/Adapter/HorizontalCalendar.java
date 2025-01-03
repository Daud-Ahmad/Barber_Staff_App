package com.qtt.barberstaffapp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qtt.barberstaffapp.R;
import com.qtt.barberstaffapp.databinding.LayoutHorizentalCalendarItemBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HorizontalCalendar {

    public interface OnDateSelectedListener {
        void onDateSelected(Calendar selectedDate);
    }

    public HorizontalCalendar(Context context, RecyclerView recyclerView, Date startDate,
                              Date endDate, OnDateSelectedListener listener) {
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        CalendarAdapter adapter = new CalendarAdapter(startDate, endDate, listener, context);
        recyclerView.setAdapter(adapter);
    }

    static class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

        private final List<Date> dates;
        private final OnDateSelectedListener listener;
        private int selectedPosition = 0;
        LayoutHorizentalCalendarItemBinding binding;
        private Context context;

        CalendarAdapter(Date startDate, Date endDate, OnDateSelectedListener listener, Context context) {
            this.dates = generateDateRange(startDate, endDate);
            this.listener = listener;
            this.context = context;
        }

        @NonNull
        @Override
        public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            binding = LayoutHorizentalCalendarItemBinding.inflate(LayoutInflater.from(context), parent, false);
            View itemView = binding.getRoot();
            return new CalendarViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
            Date date = dates.get(position);
            SimpleDateFormat sdfDay = new SimpleDateFormat("EEE");
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd");
            SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
            holder.binding.lblDate.setText(sdfDate.format(date));
            holder.binding.lblDay.setText(sdfDay.format(date));
            holder.binding.lblMonth.setText(sdfMonth.format(date));
            holder.binding.lblDate.setTextColor(selectedPosition == position ? ContextCompat.getColor(context, R.color.colorPrimary) : ContextCompat.getColor(context, R.color.colorGray));
            holder.binding.lblDay.setTextColor(selectedPosition == position ? ContextCompat.getColor(context, R.color.colorPrimary) : ContextCompat.getColor(context, R.color.colorGray));
            holder.binding.lblMonth.setTextColor(selectedPosition == position ? ContextCompat.getColor(context, R.color.colorPrimary) : ContextCompat.getColor(context, R.color.colorGray));
            holder.binding.lolParent.setBackground(selectedPosition == position ? ContextCompat.getDrawable(context, R.drawable.border_line) : null);

            holder.itemView.setOnClickListener(v -> {
                selectedPosition = position;
                notifyDataSetChanged();
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.setTime(date);
                listener.onDateSelected(selectedCalendar);
            });
        }

        @Override
        public int getItemCount() {
            return dates.size();
        }

        private List<Date> generateDateRange(Date startDate, Date endDate) {
            List<Date> dateList = new ArrayList<>();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            while (!calendar.getTime().after(endDate)) {
                dateList.add(calendar.getTime());
                calendar.add(Calendar.DATE, 1);
            }
            return dateList;
        }

        static class CalendarViewHolder extends RecyclerView.ViewHolder {
            LayoutHorizentalCalendarItemBinding binding;

            CalendarViewHolder(@NonNull View itemView) {
                super(itemView);
                binding = LayoutHorizentalCalendarItemBinding.bind(itemView);
            }
        }
    }
}

