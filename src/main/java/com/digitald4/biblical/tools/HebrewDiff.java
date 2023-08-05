package com.digitald4.biblical.tools;

import com.digitald4.common.util.Calculate;

public class HebrewDiff {
  public static void main(String[] args) {
    System.out.println("\nIsa 9:6" +
        Calculate.getDiffHtml(
            "כי ילד יולד לנו בן נתן לנו ותהי המשורה על שכמו וקרא שמו פלא יועץ אל גבור אבי עד שר השלום",
            "כי־ילד ילד־לנו בן נתן־לנו ותהי המשרה על־שכמו ויקרא שמו פלא יועץ אל גבור אבי עד שר־שלום׃"));
    System.out.println("\nIsa 61:8" +
        Calculate.getDiffHtml(
            "כיא אני יהוה אוהב משפט ושונה גזיל בעולה ונתתי פעולתכם באמת וברית עולם אכרות לכמה",
            "כי אני יהוה אהב משפט שנא גזל בעולה ונתתי פעלתם באמת וברית עולם אכרו להם׃"));
    System.out.println("\nIsa 56:6" +
        Calculate.getDiffHtml(
            "ובני הנכר הנלויים אל יהוה להיות לו לעבדים ולברך את שם יהוה ושומרים את השבת מחללה ומחזיקים בבריתי",
            "ובני הנכר הנלוים על־יהוה לשרתו ולאהבה את־שם יהוה להיות לו לעבדים כל־שמר שבת מחללו ומחזיקים בבריתי׃"));
  }
}
