package bottomsup.mapbox.app.custom_view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by kuldeep on 17/01/18.
 */

public class MyCustomTextView1 extends TextView {

    public MyCustomTextView1(Context context, AttributeSet attrs)
    {
        super(context,attrs);
        this.setTypeface(Typeface.createFromAsset(context.getAssets(),"Fonts/Roboto-Light.ttf"));
    }
}
