package dev.idlesign.fp30xcompanion;

import java.io.IOException;
import java.util.List;

public final class PerformanceController {
    public interface Listener {
        void onPerformanceChanged(PerformanceState state, Preset activePreset, boolean dirty);

        void onPerformanceMessage(String message);

        void onPerformanceError(String message);
    }

    private final PresetRepository repository;
    private final PerformanceOutput connection;
    private final Listener listener;
    private PerformanceState state;
    private Preset activePreset;
    private boolean dirty;
    private Preset resetPresetSnapshot;
    private PerformanceState resetStateSnapshot;
    private boolean resetDirtySnapshot;
    private MetronomeState metronomeState = MetronomeState.defaults();

    public PerformanceController(
            PresetRepository repository,
            PerformanceOutput connection,
            Listener listener
    ) {
        this.repository = repository;
        this.connection = connection;
        this.listener = listener;
        activePreset = repository.activePreset();
        state = activePreset.state;
    }

    public PerformanceState state() {
        return state;
    }

    public Preset activePreset() {
        return activePreset;
    }

    public boolean isDirty() {
        return dirty;
    }

    public MetronomeState metronomeState() {
        return metronomeState;
    }

    public void updateMetronome(MetronomeState updated) {
        metronomeState = updated;
        previewMetronome(updated);
        listener.onPerformanceChanged(state, activePreset, dirty);
    }

    public void previewPerformance(PerformanceState preview) {
        if (connection.isConnected()) {
            connection.applyPerformanceState(preview);
        }
    }

    public void previewMetronome(MetronomeState preview) {
        if (connection.isConnected()) {
            connection.applyMetronomeState(preview);
        }
    }

    public void cancelSettingsPreview() {
        previewPerformance(state);
        previewMetronome(metronomeState);
    }

    public void connectionReady() {
        connection.applyPerformanceState(state);
        listener.onPerformanceChanged(state, activePreset, dirty);
        listener.onPerformanceMessage(dirty
                ? "Current setup applied"
                : "Profile applied: " + activePreset.name);
    }

    public void assignSingle(Tone tone) {
        update(state.assignSingle(tone));
    }

    public void assignPrimary(Tone tone) {
        update(state.assignPrimary(tone));
    }

    public void assignLayer(Tone tone) {
        update(state.assignLayer(tone));
    }

    public void assignLeft(Tone tone) {
        update(state.assignLeft(tone));
    }

    public void remove(ToneRole role) {
        update(state.remove(role, repository.defaultPreset().state.primaryTone));
    }

    public void updateSettings(PerformanceState updated) {
        update(updated);
    }

    public List<Preset> presets() {
        return repository.all();
    }

    public void load(String presetId) {
        activePreset = repository.activate(presetId);
        state = activePreset.state;
        dirty = false;
        applyIfConnected();
        listener.onPerformanceChanged(state, activePreset, false);
    }

    public void resetToDefault() {
        resetPresetSnapshot = activePreset;
        resetStateSnapshot = state;
        resetDirtySnapshot = dirty;
        activePreset = repository.resetToDefault();
        state = activePreset.state;
        dirty = false;
        applyIfConnected();
        listener.onPerformanceChanged(state, activePreset, false);
    }

    public boolean undoReset() {
        if (resetPresetSnapshot == null || resetStateSnapshot == null) {
            return false;
        }
        activePreset = repository.activate(resetPresetSnapshot.id);
        state = resetStateSnapshot;
        dirty = resetDirtySnapshot;
        resetPresetSnapshot = null;
        resetStateSnapshot = null;
        applyIfConnected();
        listener.onPerformanceChanged(state, activePreset, dirty);
        return true;
    }

    public Preset save(String name) {
        try {
            activePreset = repository.save(activePreset.id, name, state);
            dirty = false;
            listener.onPerformanceChanged(state, activePreset, false);
            return activePreset;
        } catch (IOException error) {
            listener.onPerformanceError(error.getMessage());
            return null;
        }
    }

    public Preset duplicate() {
        return duplicate(activePreset.id);
    }

    public Preset duplicate(String presetId) {
        try {
            activePreset = repository.duplicate(presetId);
            state = activePreset.state;
            dirty = false;
            listener.onPerformanceChanged(state, activePreset, false);
            return activePreset;
        } catch (IOException error) {
            listener.onPerformanceError(error.getMessage());
            return null;
        }
    }

    public void delete(String presetId) {
        try {
            repository.delete(presetId);
            activePreset = repository.activePreset();
            state = activePreset.state;
            dirty = false;
            applyIfConnected();
            listener.onPerformanceChanged(state, activePreset, false);
        } catch (IOException error) {
            listener.onPerformanceError(error.getMessage());
        }
    }

    private void update(PerformanceState updated) {
        state = updated;
        dirty = true;
        applyIfConnected();
        listener.onPerformanceChanged(state, activePreset, true);
    }

    private void applyIfConnected() {
        if (connection.isConnected()) {
            connection.applyPerformanceState(state);
        }
    }
}
