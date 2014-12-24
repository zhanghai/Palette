/*
 * Copyright (c) 2014 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.palette;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends ActionBarActivity implements Palette.PaletteAsyncListener {

    private static final String KEY_IMAGE_URI = "image_uri";

    private static final int REQUEST_CODE_PICK_IMAGE = 0;

    private ImageView image;
    private View vibrantView;
    private View vibrantLightView;
    private View vibrantDarkView;
    private View mutedView;
    private View mutedLightView;
    private View mutedDarkView;

    private SystemBarTintManager systemBarTintManager;

    private Uri imageUri;

    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        image = (ImageView)findViewById(R.id.image);
        vibrantView = findViewById(R.id.vibrant);
        vibrantLightView = findViewById(R.id.vibrant_light);
        vibrantDarkView = findViewById(R.id.vibrant_dark);
        mutedView = findViewById(R.id.muted);
        mutedLightView = findViewById(R.id.muted_light);
        mutedDarkView = findViewById(R.id.muted_dark);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setTranslucentStatus(true);
        }
        systemBarTintManager = new SystemBarTintManager(this);
        systemBarTintManager.setTintColor(getResources().getColor(resolveAttribute(
                R.attr.colorPrimary)));
        systemBarTintManager.setStatusBarTintEnabled(true);
        systemBarTintManager.setNavigationBarTintEnabled(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(KEY_IMAGE_URI, imageUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Uri uri = savedInstanceState.getParcelable(KEY_IMAGE_URI);
        if (uri != null) {
            onImagePicked(uri);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_pick_image:
                onPickImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            if (resultCode == RESULT_OK) {
                onImagePicked(data.getData());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onGenerated(Palette palette) {
        int transparent = getResources().getColor(android.R.color.transparent);
        int vibrant = palette.getVibrantColor(transparent);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(vibrant));
        systemBarTintManager.setTintColor(vibrant);
        vibrantView.setBackgroundColor(vibrant);
        vibrantLightView.setBackgroundColor(palette.getLightVibrantColor(transparent));
        vibrantDarkView.setBackgroundColor(palette.getDarkVibrantColor(transparent));
        mutedView.setBackgroundColor(palette.getMutedColor(transparent));
        mutedLightView.setBackgroundColor(palette.getLightMutedColor(transparent));
        mutedDarkView.setBackgroundColor(palette.getDarkMutedColor(transparent));
    }

    private void onPickImage() {

        Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickImageIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(pickImageIntent,
                getString(R.string.action_pick_image));
        startActivityForResult(chooserIntent, REQUEST_CODE_PICK_IMAGE);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void onImagePicked(Uri uri) {

        Bitmap newBitmap;
        try {
            newBitmap = decodeBitmapUri(uri);
            if (newBitmap == null) {
                Toast.makeText(this, R.string.decode_failed, Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.io_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (bitmap != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                image.setBackground(null);
            } else {
                //noinspection deprecation
                image.setBackgroundDrawable(null);
            }
            bitmap.recycle();
        }
        bitmap = newBitmap;
        image.setImageBitmap(bitmap);
        Palette.generateAsync(bitmap, this);

        imageUri = uri;
    }

    @TargetApi(19)
    private void setTranslucentStatus(boolean translucentStatus) {
        Window window = getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        if (translucentStatus) {
            attributes.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        } else {
            attributes.flags &= ~WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        }
        window.setAttributes(attributes);
    }

    private int resolveAttribute(int attribute) {
        TypedArray attributes = obtainStyledAttributes(new int[]{attribute});
        int resId = attributes.getResourceId(0, -1);
        attributes.recycle();
        return resId;
    }

    private Bitmap decodeBitmapUri(Uri uri) throws IOException {

        InputStream inputStream = getContentResolver().openInputStream(uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        inputStream = getContentResolver().openInputStream(uri);
        // Canvas.getMaximumBitmapWidth() needs a hardware accelerated canvas to produce the right
        // result, so we simply use 2048x2048 instead.
        options.inSampleSize = computeInSampleSize(options, 2048, 2048);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        return bitmap;
    }

    private int computeInSampleSize(BitmapFactory.Options options, int maxWidth, int maxHeight) {

        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;

        while (width > maxWidth || height > maxHeight) {
            inSampleSize *= 2;
            width /= 2;
            height /= 2;
        }

        return inSampleSize;
    }
}
