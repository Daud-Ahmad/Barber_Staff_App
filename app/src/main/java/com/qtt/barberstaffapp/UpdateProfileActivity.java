package com.qtt.barberstaffapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.qtt.barberstaffapp.Common.Common;
import com.qtt.barberstaffapp.Common.LoadingDialog;
import com.qtt.barberstaffapp.databinding.ActivityUpdateProfileBinding;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UpdateProfileActivity extends AppCompatActivity {
    ActivityUpdateProfileBinding binding;
    Uri fileUri;
    private LoadingDialog dialog;
    StorageReference storageReference;
    private static final int REQUEST_CAMERA_PERMISSION = 102;

    private String currentPhotoPath;

    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    File file = new File(currentPhotoPath);
                    if (file.exists()) {
                        binding.imgUserAvatar.setImageURI(Uri.fromFile(file));
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateProfileBinding.inflate(getLayoutInflater());

        getWindow().setStatusBarColor(this.getResources().getColor(R.color.colorPrimary));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Profile");

        dialog = new LoadingDialog(this);

        initView();
        setContentView(binding.getRoot());
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

        binding.edtUserName.setText(Common.currentBarber.getName());
        binding.edtUserAddress.setText(Common.currentBarber.getAddress());
        binding.edtUserPhone.setText(Common.currentBarber.getPhone());


        if (Common.currentBarber.getAvatar() != null && !Common.currentBarber.getAvatar().isEmpty()) {
            Picasso.get().load(Common.currentBarber.getAvatar()).error(R.drawable.user_avatar).into(binding.imgUserAvatar);
        }

        binding.imgAddAvatar.setOnClickListener(v -> {
            if (checkAndRequestCameraPermission()) {
                dispatchTakePictureIntent();
            }
        });

        binding.btnUpdate.setOnClickListener(v -> {
            dialog.show();
            if (Common.currentBarber.getName() == null || !Common.currentBarber.getName().equals(binding.edtUserName.getText().toString())) {
                Common.currentBarber.setName(binding.edtUserName.getText().toString());

                FirebaseFirestore.getInstance().collection("AllSalon")
                        .document(Common.stateName)
                        .collection("Branch")
                        .document(Common.selectedSalon.getId())
                        .collection("Barbers")
                        .document(Common.currentBarber.getBarberId())
                        .update("name", binding.edtUserName.getText().toString())
                        .addOnCompleteListener(task13 -> {
                            Log.d("Update_profile", "update name: successfully");
                        }).addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            if (Common.currentBarber.getAddress() == null || !Common.currentBarber.getAddress().equals(binding.edtUserAddress.getText().toString())) {
                Common.currentBarber.setAddress(binding.edtUserAddress.getText().toString());

                FirebaseFirestore.getInstance().collection("AllSalon")
                        .document(Common.stateName)
                        .collection("Branch")
                        .document(Common.selectedSalon.getId())
                        .collection("Barbers")
                        .document(Common.currentBarber.getBarberId())
                        .update("address", binding.edtUserAddress.getText().toString())
                        .addOnCompleteListener(task13 -> {
                            Log.d("Update_profile", "update address: successfully");
                        }).addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            if (fileUri != null) {
                upLoadPicture(fileUri);
            }

            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            Toast.makeText(this, "Update successfully", Toast.LENGTH_SHORT).show();

            binding.edtUserName.setText(Common.currentBarber.getName());
            binding.edtUserAddress.setText(Common.currentBarber.getAddress());
            binding.edtUserPhone.setText(Common.currentBarber.getPhone());


        });
    }

    private void upLoadPicture(Uri fileUri) {
        if (fileUri != null) {
            dialog.show();

            String fileName = Common.getFileName(getContentResolver(), fileUri);
            String path = new StringBuilder("User_Avatar/").append(fileName).toString();

            storageReference = FirebaseStorage.getInstance().getReference(path);

            UploadTask uploadTask = storageReference.putFile(fileUri);

            Task<Uri> task = uploadTask.continueWithTask(task1 -> {
                if (!task1.isSuccessful()) {
                    Toast.makeText(UpdateProfileActivity.this, "Failed to upload picture!", Toast.LENGTH_SHORT).show();
                }

                return storageReference.getDownloadUrl();

            }).addOnCompleteListener(task12 -> {
                if (task12.isSuccessful()) {
                    String url = task12.getResult().toString().substring(0, task12.getResult().toString().indexOf("&token"));
                    Log.d("AAAAA", "download: " + url);


                    ///AllSalon/Florida/Branch/0n7ikrtgQXW4EXhuJ0qy/Barbers/UyQvnFQSQ45PJ26FuT8L
                    FirebaseFirestore.getInstance().collection("AllSalon")
                            .document(Common.stateName)
                            .collection("Branch")
                            .document(Common.selectedSalon.getId())
                            .collection("Barbers")
                            .document(Common.currentBarber.getBarberId())
                            .update("avatar", url)
                            .addOnCompleteListener(task13 -> {
                                Log.d("Update_profile", "upLoadPicture: successfully");
                                Common.currentBarber.setAvatar(url);
                                Picasso.get().load(Common.currentBarber.getAvatar()).error(R.drawable.user_avatar).into(binding.imgUserAvatar);
                                dialog.dismiss();
                            }).addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).addOnFailureListener(e -> {
                dialog.dismiss();
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private boolean checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return false;
        }
        return true;
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
}