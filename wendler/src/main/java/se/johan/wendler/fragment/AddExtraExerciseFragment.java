package se.johan.wendler.fragment;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import com.melnykov.fab.FloatingActionButton;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleFloatViewManager;
import com.williammora.snackbar.Snackbar;

import java.sql.SQLException;
import java.util.ArrayList;

import se.johan.wendler.R;
import se.johan.wendler.ui.adapter.AdditionalExerciseAdapter;
import se.johan.wendler.model.AdditionalExercise;
import se.johan.wendler.model.TapToUndoItem;
import se.johan.wendler.model.Workout;
import se.johan.wendler.sql.SqlHandler;
import se.johan.wendler.ui.dialog.AdditionalExerciseDialog;
import se.johan.wendler.util.CardsOptionHandler;
import se.johan.wendler.util.StringHelper;
import se.johan.wendler.util.WendlerizedLog;

/**
 * Fragments which enables adding additional mExercises to workouts.
 * TODO MAKE THIS SAME AS WORKOUT ADDITIONAL
 */
public class AddExtraExerciseFragment extends Fragment implements
        DragSortListView.DropListener,
        View.OnClickListener,
        DragSortListView.RemoveListener,
        AdditionalExerciseDialog.onConfirmClickedListener {

    private static final String EXTRA_KEY_NAME = "keyName";

    private ArrayList<AdditionalExercise> mExercises;

    private AdditionalExerciseAdapter mAdapter;

    public AddExtraExerciseFragment() {
    }

    /**
     * Static creation to avoid problems on rotation.
     */
    public static AddExtraExerciseFragment newInstance(String name) {

        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_KEY_NAME, name);
        AddExtraExerciseFragment fragment = new AddExtraExerciseFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    /**
     * Called when the view is created.
     */
    @SuppressLint("WrongViewCast")
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_drag, container, false);

        SqlHandler sqlHandler = new SqlHandler(getActivity());
        try {
            sqlHandler.open();
            Workout workout = new Workout(
                    getArguments().getString(EXTRA_KEY_NAME),
                    StringHelper.getTranslatableName(getActivity(),
                            getArguments().getString(EXTRA_KEY_NAME))
            );

            mExercises = sqlHandler.getExtraExerciseForWorkout(workout);
        } catch (SQLException e) {
            WendlerizedLog.e("Failed to get extra mExercises for " +
                    getArguments().getString(EXTRA_KEY_NAME), e);
        } finally {
            sqlHandler.close();
        }

        mAdapter = new AdditionalExerciseAdapter(
                getActivity(),
                mExercises,
                mOptionsHandler,
                AdditionalExerciseAdapter.TYPE_ADD_WORKOUT);

        DragSortListView dragSortListView = (DragSortListView) view.findViewById(R.id.list_drag);
        dragSortListView.setAdapter(mAdapter);
        dragSortListView.setDragEnabled(true);
        dragSortListView.setDropListener(this);
        buildController(dragSortListView);
        FloatingActionButton floatingActionButton =
                (FloatingActionButton) view.findViewById(R.id.button_floating_action);
        floatingActionButton.setOnClickListener(this);

        return view;
    }

    /**
     * Called when we click on the footer.
     */
    @Override
    public void onClick(View v) {
        launchAddDialog(null, getNextExtraExerciseId());
    }

    /**
     * Called when the ListView is reordered.
     */
    @Override
    public void drop(int from, int to) {
        AdditionalExercise exercise = mExercises.get(from);
        mExercises.remove(exercise);
        mExercises.add(to, exercise);
        SqlHandler sql = new SqlHandler(getActivity());
        try {
            sql.open();
            sql.doReorderAdditionalExercise(mExercises, getArguments().getString(EXTRA_KEY_NAME));
        } catch (SQLException e) {
            WendlerizedLog.e("Failed to reorder exercises after drop", e);
        } finally {
            sql.close();
        }
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Called when an exercise is removed.
     */
    @Override
    public void remove(int which) {
        onRemove(which);
    }

    /**
     * Called when we confirm adding an additional exercise.
     */
    @Override
    public void onConfirmClicked(AdditionalExercise exercise) {

        SqlHandler sqlHandler = new SqlHandler(getActivity());

        try {
            sqlHandler.open();
            int pos = getPosForAdditionalExercise(exercise.getExerciseId());
            boolean isNew = sqlHandler.extraExerciseIsNew(
                    getArguments().getString(EXTRA_KEY_NAME), exercise.getExerciseId())
                    && pos == -1;

            if (isNew) {
                mExercises.add(exercise);
            } else if (pos > -1) {
                mExercises.set(pos, exercise);
            }

            sqlHandler.storeAdditionalExercise(
                    exercise,
                    getArguments().getString(EXTRA_KEY_NAME),
                    isNew,
                    pos == -1 ? mExercises.size() - 1 : pos);
        } catch (SQLException e) {
            WendlerizedLog.e("Failed to update additional exercise", e);
        } finally {
            sqlHandler.close();
        }

        mAdapter.notifyDataSetChanged();
    }

    /**
     * Build the controller for the ListView.
     */
    private void buildController(DragSortListView dragSortListView) {
        DragSortController controller = new DragSortController(dragSortListView);
        controller.setDragHandleId(R.id.drag_handle);
        SimpleFloatViewManager simpleFloatViewManager = new SimpleFloatViewManager(dragSortListView);
        simpleFloatViewManager.setBackgroundColor(Color.TRANSPARENT);
        dragSortListView.setFloatViewManager(simpleFloatViewManager);
        dragSortListView.setRemoveListener(this);
    }

    /**
     * Called to launch the adding additional exercise dialog.
     */
    private void launchAddDialog(AdditionalExercise exercise, int id) {
        AdditionalExerciseDialog.newInstance(
                getString(R.string.add_exercise),
                exercise,
                id,
                this).show(getActivity().getSupportFragmentManager().beginTransaction(),
                AdditionalExerciseDialog.TAG);

    }

    /**
     * Return the next available additional exercise id.
     */
    private int getNextExtraExerciseId() {
        int id = 0;
        for (AdditionalExercise exercise : mExercises) {
            if (exercise.getExerciseId() > id) {
                id = exercise.getExerciseId();
            }
        }
        return ++id;
    }

    /**
     * Called to remove an additional exercise.
     */
    private void onRemove(int which) {
        AdditionalExercise exercise = mExercises.get(which);
        mExercises.remove(which);

        TapToUndoItem item = new TapToUndoItem(exercise, which);
        createSnackBar(exercise, item);
        mAdapter.notifyDataSetChanged();

        SqlHandler sqlHandler = new SqlHandler(getActivity());
        try {
            sqlHandler.open();
            sqlHandler.deleteAdditionalExercise(getArguments().getString(EXTRA_KEY_NAME), exercise);
        } catch (SQLException e) {
            WendlerizedLog.e("Failed to remove additional exercise", e);
        } finally {
            sqlHandler.close();
        }
    }

    /**
     * Create a snack bar where the user can undo the deletion.
     */
    private void createSnackBar(AdditionalExercise exercise, TapToUndoItem item) {
        Snackbar.with(getActivity())
                .text(getSnackBarText(exercise))
                .actionLabel(getString(R.string.undo))
                .actionListener(getActionListener(item))
                .eventListener(getEventListener())
                .show(getActivity());
    }

    /**
     * Returns the EventListener for displaying and hiding the snack bar
     */
    private Snackbar.EventListener getEventListener() {
        final View view = getActivity().findViewById(R.id.button_floating_action);
        return new Snackbar.EventListener() {
            @Override
            public void onShow(int height) {
                view.animate()
                        .translationY(view.getTranslationY() - (height * 2))
                        .setInterpolator(getInterpolator())
                        .start();
            }

            @Override
            public void onDismiss(int height) {
                view.animate().translationY(view.getTranslationY() + (height * 2)).start();
            }
        };
    }

    /**
     * Load the interpolator used for animating the FAB up.
     */
    private Interpolator getInterpolator() {
        return AnimationUtils.loadInterpolator(
                getActivity(), android.R.interpolator.decelerate_quad);
    }

    /**
     * Returns an ActionListener for undoing the deletion.
     */
    private Snackbar.ActionClickListener getActionListener(final TapToUndoItem item) {
        return new Snackbar.ActionClickListener() {
            @Override
            public void onActionClicked() {
                onUndo(item);
            }
        };
    }

    /**
     * Called to undo a deletion of an exercise.
     */
    private void onUndo(Parcelable token) {
        TapToUndoItem item = (TapToUndoItem) token;
        AdditionalExercise exercise = (AdditionalExercise) item.getObject();
        mExercises.add(item.getPosition(), exercise);
        mAdapter.notifyDataSetChanged();

        SqlHandler sqlHandler = new SqlHandler(getActivity());
        try {
            sqlHandler.open();
            sqlHandler.storeAdditionalExercise(
                    exercise,
                    getArguments().getString(EXTRA_KEY_NAME),
                    true,
                    getPosForAdditionalExercise(exercise.getExerciseId()));
        } catch (SQLException e) {
            WendlerizedLog.e("Failed to store exercise after undo", e);
        } finally {
            sqlHandler.close();
        }
    }

    /**
     * Returns the text for the snack bar.
     */
    private String getSnackBarText(AdditionalExercise exercise) {
        return String.format(getString(R.string.snack_bar_deleted), exercise.getName());
    }

    /**
     * Return the position of an additional exercise. If it doesn't exist return -1.
     */
    private int getPosForAdditionalExercise(int id) {
        for (int i = 0; i < mExercises.size(); i++) {
            if (mExercises.get(i).getExerciseId() == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Handler for clicks on the items on additional exercise cards.
     */
    private final CardsOptionHandler mOptionsHandler = new CardsOptionHandler() {
        @Override
        public void onDelete(int position) {
            onRemove(position);
        }

        @Override
        public void onEdit(int position) {
            AdditionalExercise exercise = mExercises.get(position);
            launchAddDialog(exercise, exercise.getExerciseId());
        }
    };


}
