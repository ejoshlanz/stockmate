package com.joshlanz.stockmate.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.joshlanz.stockmate.statics.Constants;
import com.joshlanz.stockmate.database.AppDatabase;
import com.joshlanz.stockmate.database.ItemDao;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.joshlanz.stockmate.R;
import com.joshlanz.stockmate.adapters.ItemAdapter;
import com.joshlanz.stockmate.database.RecentActivityDao;
import com.joshlanz.stockmate.models.Item;
import com.joshlanz.stockmate.models.RecentActivity;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class InventoryFragment extends Fragment implements ItemAdapter.OnItemActionListener {
    private AppDatabase db;
    private ItemDao itemDao;
    private ItemAdapter adapter;
    private RecyclerView recyclerView;
    private final List<Item> itemList = new ArrayList<>();
    private final List<Item> filteredList = new ArrayList<>();
    private AutoCompleteTextView categoryDropdown, sortByDropdown;
    private final List<String> categories = new ArrayList<>();
    private final List<String> sortOptions = Arrays.asList("Default", "Sort by Name", "Sort by Price", "Sort by Date");

    private RecentActivityDao recentActivityDao;

    private ImageView tempImageView;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    private interface ImageResultHandler {
        void onImageSelected(Uri uri);
        void onImageCaptured(Bitmap bitmap);
    }

    private ImageResultHandler currentImageResultHandler = null;

    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private static final int REQUEST_CAMERA_PERMISSION = 100;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        db = AppDatabase.getDatabase(requireContext());
        itemDao = db.itemDao();

        loadCategories();

        View view = inflater.inflate(R.layout.fragment_inventory, container, false);

        recyclerView = view.findViewById(R.id.rv_items);
        TextView totalItemsText = view.findViewById(R.id.total_items_count);
        TextView lowStockText = view.findViewById(R.id.low_stock_count);
        TextInputEditText searchBar = view.findViewById(R.id.search_bar);
        categoryDropdown = view.findViewById(R.id.category_dropdown);
        sortByDropdown = view.findViewById(R.id.sort_by_dropdown);
        recentActivityDao = db.recentActivityDao();


        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ItemAdapter(filteredList, this);

        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());



        registerResultLaunchers();
        loadItems();

        int totalItems = itemList.size();
        int lowStock = (int) itemList.stream().filter(i -> i.getStock() < Constants.LOW_STOCK_THRESHOLD).count();

        totalItemsText.setText(String.valueOf(totalItems));
        lowStockText.setText(String.valueOf(lowStock));

        setupDropdowns();
        setupSearch(searchBar);

        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> showAddItemDialog());

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(Constants.ACTION_LOW_STOCK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // API 33+
            requireContext().registerReceiver(lowStockReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(lowStockReceiver, filter);
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        requireContext().unregisterReceiver(lowStockReceiver);
    }
    private BroadcastReceiver lowStockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_LOW_STOCK.equals(intent.getAction())) {
                ArrayList<String> lowStockItemNames = intent.getStringArrayListExtra("lowStockItemNames");
                if (lowStockItemNames != null && !lowStockItemNames.isEmpty()) {
                    // Join the list into a single string separated by commas
                    String message = TextUtils.join(", ", lowStockItemNames);

                    Toast.makeText(requireContext(), "Low stock: " + message, Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                launchCamera();
            } else {
                Toast.makeText(getContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        }
    }



    // Register launchers for gallery and camera
    private void registerResultLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && currentImageResultHandler != null) {
                        Uri selectedUri = result.getData().getData();
                        currentImageResultHandler.onImageSelected(selectedUri);
                    }
                    currentImageResultHandler = null;
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && currentImageResultHandler != null) {
                        Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                        currentImageResultHandler.onImageCaptured(photo);
                    }
                    currentImageResultHandler = null;
                });
    }

    @Override
    public void onEditClick(Item item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Item");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_item, null);
        EditText itemName = dialogView.findViewById(R.id.item_name);
        EditText itemCategory = dialogView.findViewById(R.id.item_category);
        EditText itemStock = dialogView.findViewById(R.id.item_stock);
        EditText itemPrice = dialogView.findViewById(R.id.item_price);
        ImageView imageView = dialogView.findViewById(R.id.item_image);
        Button btnCamera = dialogView.findViewById(R.id.btn_camera);
        Button btnFiles = dialogView.findViewById(R.id.btn_files);

        // Initialize fields
        itemName.setText(item.getTitle());
        itemCategory.setText(item.getCategory());
        itemStock.setText(String.valueOf(item.getStock()));
        itemPrice.setText(String.valueOf(item.getPrice()));

        Glide.with(requireContext()).load(item.getImageUri()).into(imageView);

        AtomicReference<Uri> dialogImageUri = new AtomicReference<>(item.getImageUri());

        btnCamera.setOnClickListener(v -> {
            currentImageResultHandler = new ImageResultHandler() {
                @Override
                public void onImageSelected(Uri uri) {
                    dialogImageUri.set(uri);
                    Glide.with(requireContext()).load(uri).into(imageView);
                }
                @Override
                public void onImageCaptured(Bitmap bitmap) {
                    Uri uri = getImageUriFromBitmap(requireContext(), bitmap);
                    dialogImageUri.set(uri);
                    imageView.setImageBitmap(bitmap);
                }
            };
            checkCameraPermissionAndLaunch();
        });

        btnFiles.setOnClickListener(v -> {
            currentImageResultHandler = new ImageResultHandler() {
                @Override
                public void onImageSelected(Uri uri) {
                    dialogImageUri.set(uri);
                    Glide.with(requireContext()).load(uri).into(imageView);
                }
                @Override
                public void onImageCaptured(Bitmap bitmap) { /* no-op for gallery */ }
            };
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(galleryIntent);
        });

        builder.setView(dialogView);

        // IMPORTANT: Use null listener here, handle later manually
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = itemName.getText().toString().trim();
            String category = itemCategory.getText().toString().trim();
            String stockStr = itemStock.getText().toString().trim();
            String priceStr = itemPrice.getText().toString().trim();

            // Validate
            if (name.isEmpty() || category.isEmpty() || stockStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(getContext(), "All fields must be filled", Toast.LENGTH_SHORT).show();
                return; // don't dismiss dialog
            }

            int stock;
            double price;
            try {
                stock = Integer.parseInt(stockStr);
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid number format for stock or price", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri imageUri = dialogImageUri.get() != null ? dialogImageUri.get() : item.getImageUri();

            // Update item data
            item.setTitle(name);
            item.setCategory(category);
            item.setStock(stock);
            item.setPrice(price);
            item.setImageUri(imageUri);

            int index = filteredList.indexOf(item);
            if (index != -1) {
                requireActivity().runOnUiThread(() -> {
                    adapter.notifyItemChanged(index);
                });
            } else {

                requireActivity().runOnUiThread(() -> {
                    adapter.notifyItemChanged(index);
                    adapter.notifyDataSetChanged();

                });
            }

            // Update DB async
            new Thread(() -> {
                itemDao.update(item);
                requireActivity().runOnUiThread(() -> {
                    loadCategories();
                    logRecentActivity(R.drawable.ic_edit, name, "Updated item: " + stock + " units • ₱" + price);
                });
            }).start();

            Toast.makeText(getContext(), "Item updated successfully", Toast.LENGTH_SHORT).show();

            dialog.dismiss(); // manually dismiss only when all OK
        });
    }



    @Override
    public void onDeleteClick(Item item) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    int position = filteredList.indexOf(item);
                    if (position != -1) {
                        // Remove from lists
                        filteredList.remove(position);
                        itemList.remove(item);

                        // Notify adapter incrementally for smooth animation
                        adapter.notifyItemRemoved(position);
                        recyclerView.scheduleLayoutAnimation();

                        // Delete from database asynchronously
                        new Thread(() -> {
                            itemDao.delete(item);
                            requireActivity().runOnUiThread(() -> {
                                loadCategories();
                                logRecentActivity(R.drawable.ic_bin, item.getTitle(), "Deleted item");
                            });
                        }).start();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }



    private void setupDropdowns() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categories);
        categoryDropdown.setAdapter(categoryAdapter);
        categoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selected = categories.get(position);
            if (selected.equals("Default")) resetCategoryFilter();
            else filterByCategory(selected);
        });

        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, sortOptions);
        sortByDropdown.setAdapter(sortAdapter);
        sortByDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selected = sortOptions.get(position);
            if (selected.equals("Default")) resetSortFilter();
            else sortItems(selected);
        });
    }

    private void setupSearch(TextInputEditText searchBar) {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadItems() {
        new Thread(() -> {
            List<Item> dbItems = itemDao.getAllItems();
            requireActivity().runOnUiThread(() -> {
                itemList.clear();
                itemList.addAll(dbItems);
                filteredList.clear();
                filteredList.addAll(dbItems);
                adapter.notifyItemInserted(itemList.size() - 1);
                recyclerView.scheduleLayoutAnimation();
                updateInventoryStats(); // <-- update counts from DB
            });
        }).start();
    }




    private void filterItems(String query) {
        List<Item> filtered = new ArrayList<>();
        for (Item item : itemList) {
            if (item.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(item);
            }
        }
        filteredList.clear();
        filteredList.addAll(filtered);
        adapter.notifyDataSetChanged();
    }

    private void filterByCategory(String category) {
        filteredList.clear();
        for (Item item : itemList) {
            if (item.getCategory().equals(category)) {
                filteredList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void resetCategoryFilter() {
        filteredList.clear();
        filteredList.addAll(itemList);
        adapter.notifyDataSetChanged();
    }

    private void sortItems(String sortOption) {
        switch (sortOption) {
            case "Sort by Name":
                filteredList.sort(Comparator.comparing(Item::getTitle));
                break;
            case "Sort by Price":
                filteredList.sort(Comparator.comparing(Item::getPrice));
                break;
//            case "Sort by Date":
//                filteredList.sort(Comparator.comparing(Item::getDateAdded));
//                break;
            default:
                break;
        }
        adapter.notifyDataSetChanged();
    }

    private void resetSortFilter() {
        filteredList.clear();
        filteredList.addAll(itemList);
        adapter.notifyDataSetChanged();
    }

    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Item");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_item, null);
        EditText itemName = dialogView.findViewById(R.id.item_name);
        EditText itemCategory = dialogView.findViewById(R.id.item_category);
        EditText itemStock = dialogView.findViewById(R.id.item_stock);
        EditText itemPrice = dialogView.findViewById(R.id.item_price);
        tempImageView = dialogView.findViewById(R.id.item_image);
        Button btnCamera = dialogView.findViewById(R.id.btn_camera);
        Button btnFiles = dialogView.findViewById(R.id.btn_files);

        AtomicReference<Uri> addDialogImageUri = new AtomicReference<>(null);

        btnCamera.setOnClickListener(v -> {
            currentImageResultHandler = new ImageResultHandler() {
                @Override
                public void onImageSelected(Uri uri) {
                    addDialogImageUri.set(uri);
                    Glide.with(requireContext()).load(uri).into(tempImageView);
                }
                @Override
                public void onImageCaptured(Bitmap bitmap) {
                    Uri uri = getImageUriFromBitmap(requireContext(), bitmap);
                    addDialogImageUri.set(uri);
                    tempImageView.setImageBitmap(bitmap);
                }
            };
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(cameraIntent);
        });

        btnFiles.setOnClickListener(v -> {
            currentImageResultHandler = new ImageResultHandler() {
                @Override
                public void onImageSelected(Uri uri) {
                    addDialogImageUri.set(uri);
                    Glide.with(requireContext()).load(uri).into(tempImageView);
                }
                @Override
                public void onImageCaptured(Bitmap bitmap) { }
            };
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(galleryIntent);
        });


        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = itemName.getText().toString();
            String category = itemCategory.getText().toString();
            String stockStr = itemStock.getText().toString();
            String priceStr = itemPrice.getText().toString();

            if (name.isEmpty() || category.isEmpty() || stockStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(getContext(), "All fields must be filled", Toast.LENGTH_SHORT).show();
                return;
            }

            int stock = Integer.parseInt(stockStr);
            double price = Double.parseDouble(priceStr);

            // Convert Uri to String for imageUri
            String imageUriString = addDialogImageUri.get() != null ? addDialogImageUri.get().toString() : "android.resource://" + requireContext().getPackageName() + "/" + R.drawable.image_default;

            Item newItem = new Item(name, category, stock, price, imageUriString);
            itemList.add(newItem);
            filteredList.add(newItem);

            new Thread(() -> {
                itemDao.insert(newItem);
                requireActivity().runOnUiThread(() -> {
                    loadItems(); // reloads list and updates stats
                    loadCategories();
                    logRecentActivity(R.drawable.ic_inventory, name, "New item: " + stock + " units" + " • ₱" + price);
                });
            }).start();



            Toast.makeText(getContext(), "Item added successfully", Toast.LENGTH_SHORT).show();
        });


        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private Uri getImageUriFromBitmap(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "TempImage", null);
        return Uri.parse(path);
    }

    private void loadCategories() {
        new Thread(() -> {
            List<String> dbCategories = itemDao.getAllCategories();
            requireActivity().runOnUiThread(() -> {
                categories.clear();
                categories.addAll(dbCategories);
            });
        }).start();
    }

    private void updateInventoryStats() {
        new Thread(() -> {
            List<Item> allItems = itemDao.getAllItems();
            int totalItems = allItems.size();
            int lowStockCount = (int) allItems.stream().filter(i -> i.getStock() < 5).count();

            requireActivity().runOnUiThread(() -> {
                TextView totalItemsText = getView().findViewById(R.id.total_items_count);
                TextView lowStockText = getView().findViewById(R.id.low_stock_count);
                totalItemsText.setText(String.valueOf(totalItems));
                lowStockText.setText(String.valueOf(lowStockCount));
            });
        }).start();
    }

    private void logRecentActivity(int iconResId, String title, String description) {
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        RecentActivity activity = new RecentActivity(iconResId, title, description, currentTime);

        new Thread(() -> {
            recentActivityDao.insert(activity);
        }).start();
    }


}
