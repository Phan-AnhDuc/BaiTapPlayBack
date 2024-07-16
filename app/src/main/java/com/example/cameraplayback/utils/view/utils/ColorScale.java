package com.example.cameraplayback.utils.view.utils;

import androidx.annotation.NonNull;

import com.example.cameraplayback.ui.theme.ColorData;
import com.example.cameraplayback.utils.view.VideoTimeBar;

import java.util.List;

public class ColorScale implements VideoTimeBar.ColorScale {
    private List<ColorData> data;

    public ColorScale(@NonNull List<ColorData> data) {
        this.data = data;
    }

    @Override
    public int getSize() {
        return data.size();
    }

    @Override
    public long getStart(int index) {
        return data.get(index).getStart();
    }

    @Override
    public long getEnd(int index) {
        return data.get(index).getEnd();
    }

    @Override
    public int getColor(int index) {
        return data.get(index).getColor();
    }

    public List<ColorData> getData() {
        return data;
    }

    public void setData(List<ColorData> data) {
        this.data = data;
    }
}

