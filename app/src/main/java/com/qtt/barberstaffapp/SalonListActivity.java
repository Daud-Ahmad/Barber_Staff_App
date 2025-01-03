package com.qtt.barberstaffapp;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qtt.barberstaffapp.Adapter.MySalonAdapter;
import com.qtt.barberstaffapp.Common.Common;
import com.qtt.barberstaffapp.Common.LoadingDialog;
import com.qtt.barberstaffapp.Common.SharedPreferencesClass;
import com.qtt.barberstaffapp.Common.SpacesItemDecoration;
import com.qtt.barberstaffapp.Interface.IBranchLoadListener;
import com.qtt.barberstaffapp.Interface.IGetBarberListener;
import com.qtt.barberstaffapp.Interface.ILoadCountSalon;
import com.qtt.barberstaffapp.Interface.IUserLoginRememberListener;
import com.qtt.barberstaffapp.Model.Barber;
import com.qtt.barberstaffapp.Model.Salon;
import com.qtt.barberstaffapp.databinding.ActivitySalonListBinding;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SalonListActivity extends AppCompatActivity implements IBranchLoadListener,
        ILoadCountSalon, IGetBarberListener, IUserLoginRememberListener {

    ActivitySalonListBinding binding;

    ILoadCountSalon iLoadCountSalon;
    IBranchLoadListener iBranchLoadListener;

    private LoadingDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySalonListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(this.getResources().getColor(R.color.colorBackground));

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        initView();

        init();

        loadSalonBaseOnCity(Common.stateName);
    }

    private void loadSalonBaseOnCity(String stateName) {
        dialog.show();

        FirebaseFirestore.getInstance().collection("AllSalon")
                .document(stateName)
                .collection("Branch")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            iLoadCountSalon.onLoadCountSalonSuccess(Objects.requireNonNull(task.getResult()).size());
                            List<Salon> salons = new ArrayList<>();

                            for (DocumentSnapshot documentSnapshot : task.getResult()) {
                                Salon salon = documentSnapshot.toObject(Salon.class);
                                salon.setId(documentSnapshot.getId());
                                salons.add(salon);
                            }

                            iBranchLoadListener.onBranchLoadSuccess(salons);
                            dialog.dismiss();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                iBranchLoadListener.onBranchLoadFailed(e.getMessage());
                dialog.dismiss();
            }
        });
    }

    private void init() {
        iBranchLoadListener = this;
        iLoadCountSalon = this;

        dialog = new LoadingDialog(this);
    }

    private void initView() {
        binding.recyclerSalon.setHasFixedSize(true);
        binding.recyclerSalon.setLayoutManager(new GridLayoutManager(this, 1));
        binding.recyclerSalon.addItemDecoration(new SpacesItemDecoration(8));
    }

    @Override
    public void onBranchLoadSuccess(List<Salon> salonList) {
        MySalonAdapter mySalonAdapter = new MySalonAdapter(this, salonList, this, this);
        binding.recyclerSalon.setAdapter(mySalonAdapter);
    }

    @Override
    public void onBranchLoadFailed(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoadCountSalonSuccess(int count) {
        binding.tvSalonCount.setText(new StringBuilder("Total: (")
                                .append(count + ") salons").toString());
    }

    @Override
    public void onGetBarberSuccess(Barber barber) {
        Common.currentBarber = barber;
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        String json = gson.toJson(barber);
        Map<String, String> resultMap = gson.fromJson(json, mapType);
        SharedPreferencesClass.saveJson(this, Common.BARBER_KEY, resultMap);
    }

    @Override
    public void onUserLoginSuccess(String user) {
        SharedPreferencesClass.saveString(this, Common.LOGED_KEY, user);
        SharedPreferencesClass.saveString(this, Common.STATE_KEY, Common.stateName);
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();

// Convert the Salon object to a JSON string, then parse it into a Map
        String json = gson.toJson(Common.selectedSalon);
        Map<String, String> resultMap = gson.fromJson(json, mapType);

        SharedPreferencesClass.saveJson(this, Common.SALON_KEY, resultMap);

//        Paper.init(this);
//        Paper.book().write(Common.LOGED_KEY, user);
//        Paper.book().write(Common.STATE_KEY, Common.stateName);
//        Paper.book().write(Common.SALON_KEY, new Gson().toJson(Common.selectedSalon));
    }
}
