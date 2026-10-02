package dev.idlesign.fp30xcompanion;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ToneCatalogDialog extends Dialog {
    public interface Listener {
        PerformanceState currentState();

        void onFilterSelected(ToneFamily family);

        void onSearchChanged(String query);

        void onAssignSingle(Tone tone);

        void onAssignPrimary(Tone tone);

        void onAssignLayer(Tone tone);

        void onAssignLeft(Tone tone);

        void onToggleFavorite(Tone tone);
    }

    private static final int MENU_SINGLE = 1;
    private static final int MENU_PRIMARY = 2;
    private static final int MENU_LAYER = 3;
    private static final int MENU_LEFT = 4;
    private final List<Tone> allTones;
    private final Set<String> favorites;
    private final ToneRole replacementRole;
    private final ToneFamily initialFamily;
    private final String initialSearch;
    private final Listener listener;
    private final int displayToneCount;
    private final ArrayList<Tone> visibleTones = new ArrayList<>();
    private final List<FilterOption> filterOptions = filterOptions();
    private CatalogAdapter adapter;
    private EditText searchView;
    private Spinner filterSpinner;
    private TextView modeView;
    private TextView countView;

    public ToneCatalogDialog(
            Context context,
            List<Tone> allTones,
            Set<String> favorites,
            ToneRole replacementRole,
            ToneFamily initialFamily,
            String initialSearch,
            Listener listener
    ) {
        super(context);
        this.allTones = allTones;
        this.favorites = favorites;
        this.replacementRole = replacementRole;
        this.initialFamily = initialFamily;
        this.initialSearch = initialSearch;
        this.listener = listener;
        displayToneCount = ToneFilter.apply(
                allTones, java.util.Collections.emptySet(), ToneFamily.ALL, "").size();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(buildContent());
        applyFilter();
    }

    @Override
    public void show() {
        super.show();
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(246, 247, 249));
        SafeArea.applyAll(root, 0, 0, 0, 0);

        LinearLayout header = new LinearLayout(getContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(8), dp(6));
        modeView = text(modeLabel(), 18, Color.rgb(24, 28, 35));
        header.addView(modeView, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button done = new Button(getContext());
        done.setText(R.string.button_done);
        UiStyles.secondaryButton(done);
        done.setOnClickListener(view -> dismiss());
        header.addView(done, new LinearLayout.LayoutParams(dp(100), dp(48)));
        root.addView(header);

        searchView = new EditText(getContext());
        searchView.setSingleLine(true);
        searchView.setHint(R.string.search_hint);
        searchView.setText(initialSearch);
        searchView.setSelection(initialSearch.length());
        searchView.setPadding(dp(14), dp(8), dp(14), dp(8));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(dp(12), 0, dp(12), dp(6));
        root.addView(searchView, searchParams);
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence sequence, int start, int before, int count) {
                listener.onSearchChanged(sequence.toString());
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        LinearLayout filters = new LinearLayout(getContext());
        filters.setPadding(dp(12), 0, dp(12), dp(6));
        filterSpinner = new Spinner(getContext());
        filterSpinner.setContentDescription(
                getContext().getString(R.string.tone_filter_content_description));
        filterSpinner.setAdapter(new FilterAdapter());
        filterSpinner.setSelection(positionOf(initialFamily));
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                listener.onFilterSelected(selectedFamily());
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        filters.addView(filterSpinner, new LinearLayout.LayoutParams(0, dp(48), 1));
        countView = text("", 13, Color.rgb(103, 111, 124));
        countView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        filters.addView(countView, new LinearLayout.LayoutParams(dp(110), dp(48)));
        root.addView(filters);

        adapter = new CatalogAdapter();
        ListView list = new ListView(getContext());
        list.setAdapter(adapter);
        list.setDividerHeight(1);
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    private void applyFilter() {
        if (adapter == null || searchView == null || filterSpinner == null) {
            return;
        }
        visibleTones.clear();
        visibleTones.addAll(ToneFilter.apply(
                allTones,
                favorites,
                selectedFamily(),
                searchView.getText().toString()));
        countView.setText(getContext().getString(
                R.string.tone_count, visibleTones.size(), displayToneCount));
        modeView.setText(modeLabel());
        adapter.notifyDataSetChanged();
    }

    private void replaceTone(Tone tone) {
        switch (replacementRole) {
            case PRIMARY -> listener.onAssignPrimary(tone);
            case LAYER -> listener.onAssignLayer(tone);
            case LEFT -> listener.onAssignLeft(tone);
        }
        dismiss();
    }

    private void showAssignmentMenu(View anchor, Tone tone) {
        PerformanceMode mode = listener.currentState().mode();
        PopupMenu menu = new PopupMenu(getContext(), anchor);
        menu.getMenu().add(0, MENU_SINGLE, 0, R.string.role_single);
        menu.getMenu().add(0, MENU_PRIMARY, 1,
                mode == PerformanceMode.SPLIT ? R.string.role_right : R.string.role_main);
        menu.getMenu().add(0, MENU_LAYER, 2, R.string.role_layer);
        menu.getMenu().add(0, MENU_LEFT, 3, R.string.role_left);
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case MENU_SINGLE -> listener.onAssignSingle(tone);
                case MENU_PRIMARY -> listener.onAssignPrimary(tone);
                case MENU_LAYER -> listener.onAssignLayer(tone);
                case MENU_LEFT -> listener.onAssignLeft(tone);
                default -> {
                    return false;
                }
            }
            applyFilter();
            return true;
        });
        menu.show();
    }

    private String assignments(Tone tone) {
        PerformanceState state = listener.currentState();
        ArrayList<String> roles = new ArrayList<>();
        if (state.primaryTone.stableId().equals(tone.stableId())) {
            int roleId = state.mode() == PerformanceMode.SINGLE
                    ? R.string.role_single
                    : state.mode() == PerformanceMode.DUAL
                    ? R.string.role_main : R.string.role_right;
            roles.add(getContext().getString(roleId).toUpperCase());
        }
        if (state.layerTone != null && state.layerTone.stableId().equals(tone.stableId())) {
            roles.add(getContext().getString(R.string.role_layer).toUpperCase());
        }
        if (state.leftTone != null && state.leftTone.stableId().equals(tone.stableId())) {
            roles.add(getContext().getString(R.string.role_left).toUpperCase());
        }
        return String.join(", ", roles);
    }

    private String modeLabel() {
        return switch (listener.currentState().mode()) {
            case SINGLE -> getContext().getString(R.string.mode_single);
            case DUAL -> getContext().getString(R.string.mode_dual);
            case SPLIT -> getContext().getString(R.string.mode_split);
        };
    }

    private String displayCategory(String category) {
        return switch (category) {
            case "Piano" -> getContext().getString(R.string.category_piano);
            case "E.Piano" -> getContext().getString(R.string.category_epiano);
            case "Other" -> getContext().getString(R.string.category_other);
            case "Drums" -> getContext().getString(R.string.category_drums);
            case "GM2" -> getContext().getString(R.string.category_gm2);
            default -> category;
        };
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }

    private ToneFamily selectedFamily() {
        Object selected = filterSpinner.getSelectedItem();
        if (selected instanceof FilterOption option && option.family != null) {
            return option.family;
        }
        return ToneFamily.ALL;
    }

    private int positionOf(ToneFamily family) {
        for (int index = 0; index < filterOptions.size(); index++) {
            if (filterOptions.get(index).family == family) {
                return index;
            }
        }
        return 0;
    }

    private List<FilterOption> filterOptions() {
        ArrayList<FilterOption> options = new ArrayList<>();
        section(options, R.string.filter_section_quick);
        option(options, R.string.category_all, ToneFamily.ALL);
        option(options, R.string.category_favorites, ToneFamily.FAVORITES);
        section(options, R.string.filter_section_keyboards);
        option(options, R.string.filter_acoustic_pianos, ToneFamily.ACOUSTIC_PIANOS);
        option(options, R.string.filter_electric_pianos, ToneFamily.ELECTRIC_PIANOS);
        option(options, R.string.filter_harpsichords_clavinets, ToneFamily.HARPSICHORDS_CLAVINETS);
        option(options, R.string.filter_mallets_bells, ToneFamily.MALLETS_BELLS);
        option(options, R.string.filter_organs_accordions, ToneFamily.ORGANS_ACCORDIONS);
        section(options, R.string.filter_section_strings_orchestra);
        option(options, R.string.filter_guitars, ToneFamily.GUITARS);
        option(options, R.string.filter_basses, ToneFamily.BASSES);
        option(options, R.string.filter_solo_strings_harps, ToneFamily.SOLO_STRINGS_HARPS);
        option(options, R.string.filter_ensembles_choirs_orchestra, ToneFamily.ENSEMBLES_CHOIRS_ORCHESTRA);
        option(options, R.string.filter_brass, ToneFamily.BRASS);
        option(options, R.string.filter_sax_reeds, ToneFamily.SAX_REEDS);
        option(options, R.string.filter_flutes_pipes, ToneFamily.FLUTES_PIPES);
        section(options, R.string.filter_section_synthesizers);
        option(options, R.string.filter_synth_leads, ToneFamily.SYNTH_LEADS);
        option(options, R.string.filter_synth_pads, ToneFamily.SYNTH_PADS);
        option(options, R.string.filter_atmospheres_textures, ToneFamily.ATMOSPHERES_TEXTURES);
        section(options, R.string.filter_section_world_rhythm);
        option(options, R.string.filter_world_instruments, ToneFamily.WORLD_INSTRUMENTS);
        option(options, R.string.filter_percussion, ToneFamily.PERCUSSION);
        option(options, R.string.filter_drum_kits, ToneFamily.DRUM_KITS);
        section(options, R.string.filter_section_sound_effects);
        option(options, R.string.filter_all_sound_effects, ToneFamily.ALL_SOUND_EFFECTS);
        option(options, R.string.filter_instrument_performance_noises,
                ToneFamily.INSTRUMENT_PERFORMANCE_NOISES);
        option(options, R.string.filter_nature, ToneFamily.NATURE);
        option(options, R.string.filter_animals, ToneFamily.ANIMALS);
        option(options, R.string.filter_household_signals, ToneFamily.HOUSEHOLD_SIGNALS);
        option(options, R.string.filter_transport_machinery, ToneFamily.TRANSPORT_MACHINERY);
        option(options, R.string.filter_people_body, ToneFamily.PEOPLE_BODY);
        option(options, R.string.filter_shots_explosions, ToneFamily.SHOTS_EXPLOSIONS);
        return options;
    }

    private void section(List<FilterOption> options, int label) {
        options.add(new FilterOption(label, null));
    }

    private void option(List<FilterOption> options, int label, ToneFamily family) {
        options.add(new FilterOption(label, family));
    }

    private static final class FilterOption {
        final int label;
        final ToneFamily family;

        FilterOption(int label, ToneFamily family) {
            this.label = label;
            this.family = family;
        }
    }

    private final class FilterAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return filterOptions.size();
        }

        @Override
        public FilterOption getItem(int position) {
            return filterOptions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position).family != null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = convertView instanceof TextView
                    ? (TextView) convertView : text("", 15, Color.rgb(24, 28, 35));
            style(view, getItem(position), false);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView view = convertView instanceof TextView
                    ? (TextView) convertView : text("", 15, Color.rgb(24, 28, 35));
            style(view, getItem(position), true);
            return view;
        }

        private void style(TextView view, FilterOption option, boolean dropdown) {
            boolean section = option.family == null;
            boolean favorite = option.family == ToneFamily.FAVORITES;
            view.setText(option.label);
            view.setGravity(Gravity.CENTER_VERTICAL);
            view.setMinHeight(dp(section ? 36 : 48));
            view.setPadding(dp(dropdown && !section ? 28 : 12), 0, dp(12), 0);
            view.setTypeface(Typeface.DEFAULT,
                    section || favorite ? Typeface.BOLD : Typeface.NORMAL);
            view.setTextSize(section ? 12 : 15);
            view.setTextColor(favorite
                    ? Color.rgb(230, 106, 44)
                    : section ? Color.rgb(103, 111, 124) : Color.rgb(24, 28, 35));
            view.setBackgroundColor(favorite
                    ? Color.rgb(255, 239, 222)
                    : section ? Color.rgb(235, 237, 241) : Color.WHITE);
        }
    }

    private final class CatalogAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return visibleTones.size();
        }

        @Override
        public Tone getItem(int position) {
            return visibleTones.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).number;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ToneRowView row = convertView instanceof ToneRowView
                    ? (ToneRowView) convertView : new ToneRowView(getContext());
            Tone tone = getItem(position);
            String details = displayCategory(tone.category);
            row.bindCatalog(
                    tone,
                    details,
                    assignments(tone),
                    favorites.contains(tone.stableId()),
                    () -> {
                        listener.onToggleFavorite(tone);
                        applyFilter();
                    },
                    () -> showAssignmentMenu(row.actionButton(), tone));
            row.setOnClickListener(view -> replaceTone(tone));
            return row;
        }
    }
}
