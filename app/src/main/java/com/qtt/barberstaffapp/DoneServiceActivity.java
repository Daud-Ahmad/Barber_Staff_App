package com.qtt.barberstaffapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.qtt.barberstaffapp.Adapter.MyCartAdapter;
import com.qtt.barberstaffapp.Adapter.MyServiceAdapter;
import com.qtt.barberstaffapp.Common.Common;
import com.qtt.barberstaffapp.Common.LoadingDialog;
import com.qtt.barberstaffapp.Common.SpacesItemDecoration;
import com.qtt.barberstaffapp.EventBus.DismissDoneServiceEvent;
import com.qtt.barberstaffapp.Fragment.ShoppingFragment;
import com.qtt.barberstaffapp.Fragment.TotalPriceFragment;
import com.qtt.barberstaffapp.Interface.IBarberServicesLoadListener;
import com.qtt.barberstaffapp.Interface.IOnShoppingItemSelected;
import com.qtt.barberstaffapp.Model.BarberServices;
import com.qtt.barberstaffapp.Model.CartItem;
import com.qtt.barberstaffapp.Model.ShoppingItem;
import com.qtt.barberstaffapp.Model.User;
import com.qtt.barberstaffapp.databinding.ActivityDoneServiceBinding;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class DoneServiceActivity extends AppCompatActivity implements IBarberServicesLoadListener,
        IOnShoppingItemSelected {

    ActivityDoneServiceBinding binding;
    private static final int REQUEST_CAMERA_PERMISSION = 101;

    private LoadingDialog dialog;
    IBarberServicesLoadListener iBarberServicesLoadListener;
    HashSet<BarberServices> servicesAdded;
    LayoutInflater inflater;
    MyCartAdapter myCartAdapter;

    Uri fileUri;

    StorageReference storageReference;
    private String currentPhotoPath;

    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    File file = new File(currentPhotoPath);
                    if (file.exists()) {
                        binding.imgCustomerHair.setImageURI(Uri.fromFile(file));
                        binding.imgCustomerHair.setVisibility(View.VISIBLE);
                        binding.btnFinish.setEnabled(true);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDoneServiceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setStatusBarColor(this.getResources().getColor(R.color.colorPrimary));

        setCustomerInformation();

        init();
        initView();
        loadBarberServices();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                fileUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                takePictureLauncher.launch(takePictureIntent);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        getSupportActionBar().setTitle("Check out");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.recyclerServices.setHasFixedSize(true);
        binding.recyclerServices.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        binding.recyclerServices.addItemDecoration(new SpacesItemDecoration(8));

        binding.recyclerCart.setHasFixedSize(true);
        binding.recyclerCart.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        binding.recyclerCart.addItemDecoration(new SpacesItemDecoration(8));

        binding.switchPicture.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.cardPicture.setVisibility(View.VISIBLE);
                binding.btnFinish.setEnabled(false);
            } else {
                binding.imgAddCustomerHair.setVisibility(View.VISIBLE);
                binding.imgCustomerHair.setVisibility(View.GONE);
                binding.cardPicture.setVisibility(View.GONE);
                binding.btnFinish.setEnabled(true);
            }
        });


        binding.addShopping.setOnClickListener(v -> {
            ShoppingFragment shoppingFragment = ShoppingFragment.getInstance(DoneServiceActivity.this);
            shoppingFragment.show(getSupportFragmentManager(), "Shopping");
        });

        binding.imgAddCustomerHair.setOnClickListener(v -> {
            if (checkAndRequestCameraPermission()) {
                dispatchTakePictureIntent();
            }
        });

        binding.btnFinish.setOnClickListener(v -> {
            if (!binding.switchPicture.isChecked()) {
                //Create fragment total price
                TotalPriceFragment totalPriceFragment = TotalPriceFragment.getInstance();
                Bundle bundle = new Bundle();
                bundle.putString(Common.SERVICES_ADDED, new Gson().toJson(Common.selectedService));
                totalPriceFragment.setArguments(bundle);
                totalPriceFragment.show(getSupportFragmentManager(), "Price");
            } else {
                upLoadPicture(fileUri);
            }
        });
    }

    private void upLoadPicture(Uri fileUri) {
        if (fileUri != null) {
            dialog.show();

            String fileName = Common.getFileName(getContentResolver(), fileUri);
            String path = new StringBuilder("Customer_Pictures/").append(fileName).toString();

            storageReference = FirebaseStorage.getInstance().getReference(path);

            UploadTask uploadTask = storageReference.putFile(fileUri);

            Task<Uri> task = uploadTask.continueWithTask(task1 -> {
                if (!task1.isSuccessful()) {
                    Toast.makeText(DoneServiceActivity.this, "Failed to upload picture!", Toast.LENGTH_SHORT).show();
                }

                return storageReference.getDownloadUrl();

            }).addOnCompleteListener(task12 -> {
                if (task12.isSuccessful()) {
                    String url = task12.getResult().toString().substring(0, task12.getResult().toString().indexOf("&token"));
                    Log.d("AAAAA", "download: " + url);
                    dialog.dismiss();

                    //Create fragment total price
                    TotalPriceFragment totalPriceFragment = TotalPriceFragment.getInstance();
                    Bundle bundle = new Bundle();
                    bundle.putString(Common.SERVICES_ADDED, new Gson().toJson(servicesAdded));
                    bundle.putString(Common.IMG_URL, url);
                    totalPriceFragment.setArguments(bundle);
                    totalPriceFragment.show(getSupportFragmentManager(), "Price");


                }
            }).addOnFailureListener(e -> {
                dialog.dismiss();
                Toast.makeText(DoneServiceActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(this, "Image is not found!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadBarberServices() {
        ///AllSalon/Florida/Branch/0n7ikrtgQXW4EXhuJ0qy/Services/
        dialog.show();
        FirebaseFirestore.getInstance()
                .collection("AllSalon")
                .document(Common.stateName)
                .collection("Branch")
                .document(Common.selectedSalon.getId())
                .collection("Services")
                .get()
                .addOnFailureListener(e -> iBarberServicesLoadListener.onBarberServicesLoadFailed(e.getMessage()))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<BarberServices> barberServices = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : task.getResult()) {
                            BarberServices barberServices1 = documentSnapshot.toObject(BarberServices.class);
                            barberServices.add(barberServices1);
                        }

                        iBarberServicesLoadListener.onBarberServicesLoadSuccess(barberServices);
                    }
                });
    }

    private void init() {
        dialog = new LoadingDialog(this);

        iBarberServicesLoadListener = this;
        servicesAdded = new HashSet<>();
        inflater = LayoutInflater.from(this);

    }

    private void setCustomerInformation() {
        binding.tvCustomerName.setText(Common.currentBookingInfo.getCustomerName());
        binding.tvCustomerPhone.setText(Common.currentBookingInfo.getCustomerPhone());

        FirebaseFirestore.getInstance()
                .collection("User")
                .whereEqualTo("phoneNumber", Common.currentBookingInfo.getCustomerPhone())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        User user = null;
                        for (DocumentSnapshot documentSnapshot : task.getResult()) {
                            user = documentSnapshot.toObject(User.class);
                        }

                        if (user != null && user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                            Picasso.get().load(user.getAvatar()).error(R.drawable.user_avatar).into(binding.imgUserAvatar);
                        }
                    }
                });
        //avatar
    }

    private boolean checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return false;
        }
        return true;
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required to use the camera.", Toast.LENGTH_SHORT).show();
            }
        }
    }



    @Override
    public void onBarberServicesLoadSuccess(final List<BarberServices> barberServicesList) {
        for (BarberServices barberServices : barberServicesList) {
            if (barberServices.getUid().equals(Common.currentBookingInfo.getBarberServiceList().get(0))) {
                Common.selectedService = barberServices;
            }
        }

        MyServiceAdapter myServiceAdapter = new MyServiceAdapter(this, barberServicesList, Common.currentBookingInfo.getBarberServiceList());
        binding.recyclerServices.setAdapter(myServiceAdapter);



        loadExtraItems();
        dialog.dismiss();
    }

    @Override
    public void onBarberServicesLoadFailed(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        dialog.dismiss();
    }

    @Override
    public void onShoppingItemSelected(ShoppingItem shoppingItem) {
        //shoppingItemList.add(shoppingItem);

        CartItem cartItem = new CartItem();
        cartItem.setProductId(shoppingItem.getId());
        cartItem.setProductImage(shoppingItem.getImage());
        cartItem.setProductName(shoppingItem.getName());
        cartItem.setProductPrice(shoppingItem.getPrice());
        cartItem.setProductQuantity(1);
        cartItem.setUserPhone(Common.currentBookingInfo.getCustomerPhone());

        if (Common.currentBookingInfo.getCartItemList() == null) {
            Common.currentBookingInfo.setCartItemList(new ArrayList<CartItem>());
        }

        boolean updateQuantityFlag = false;

        for (int i = 0; i < Common.currentBookingInfo.getCartItemList().size(); ++i) {
            CartItem cartItem1 = Common.currentBookingInfo.getCartItemList().get(i);

            if (cartItem1.getProductName().equals(shoppingItem.getName())) {
                updateQuantityFlag = true;
                CartItem updateItem = cartItem1;
                updateItem.setProductQuantity(updateItem.getProductQuantity() + 1);
                Common.currentBookingInfo.getCartItemList().set(i, updateItem);
                break;
            }
        }

        if (!updateQuantityFlag) {
            Common.currentBookingInfo.getCartItemList().add(cartItem);
            Log.d("Shopping", "onShoppingItemSelected: " + Common.currentBookingInfo.getCartItemList().size());

        }

        loadExtraItems();
        myCartAdapter.notifyDataSetChanged();

        Toast.makeText(this, "Added to extra items", Toast.LENGTH_SHORT).show();
    }

    private void loadExtraItems() {
        if (Common.currentBookingInfo.getCartItemList() != null) {
            myCartAdapter = new MyCartAdapter(Common.currentBookingInfo.getCartItemList(), this);
            binding.recyclerCart.setAdapter(myCartAdapter);

        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onDismissBottomSheetDialog(DismissDoneServiceEvent event) {
        if(event.getDismiss())
            finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
