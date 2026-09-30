package com.podhod.app;
import android.widget.*;
import android.view.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;

/** Compact, accessible like control with a warm accent distinct from navigation. */
final class PlanLikeButton extends LinearLayout {
 static final int CORAL=0xFFFF9C9F;
 final MainActivity a; final ImageView heart; final TextView label,count; final View divider;
 PlanLikeButton(MainActivity a){super(a);this.a=a;setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);setMinimumHeight(a.dp(48));setPadding(a.dp(14),a.dp(10),a.dp(14),a.dp(10));
  heart=new ImageView(a);addView(heart,new LinearLayout.LayoutParams(a.dp(22),a.dp(22)));
  label=a.title("",14);LinearLayout.LayoutParams words=new LinearLayout.LayoutParams(-2,-2);words.leftMargin=a.dp(9);addView(label,words);
  divider=new View(a);LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(a.dp(1),a.dp(16));line.leftMargin=a.dp(12);line.rightMargin=a.dp(12);addView(divider,line);
  count=a.title("",15);count.setGravity(Gravity.CENTER);count.setMinWidth(a.dp(18));addView(count,new LinearLayout.LayoutParams(-2,-2));
  setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
  for(int i=0;i<getChildCount();i++)getChildAt(i).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
 }
 void update(PlanLikes.Entry e){
  boolean busy=e.saving||e.loading;setEnabled(!busy);setSelected(e.liked);
  GradientDrawable bg=a.shape(e.liked?0xFF382D39:0xFF202D3C,24);bg.setStroke(a.dp(1),e.liked?0xFF95636F:0xFF435063);
  setBackground(new RippleDrawable(ColorStateList.valueOf(0x28FF9C9F),bg,null));
  heart.setImageDrawable(new LineIcon(e.liked?"heart-filled":"heart",CORAL));
  label.setText(AccountPanel.s(e.liked?"Liked":"Like",e.liked?"Нравится":"Лайк"));label.setTextColor(e.liked?CORAL:a.TEXT);
  divider.setBackgroundColor(e.liked?0xFF765462:0xFF485465);
  count.setText(e.saving?"…":e.ready?String.valueOf(e.count):"—");count.setTextColor(e.liked?CORAL:a.MUTED);
  setContentDescription(AccountPanel.s(e.liked?"Unlike plan":"Like plan",e.liked?"Убрать лайк":"Поставить лайк")+(e.ready?" · "+e.count:""));
 }
}
