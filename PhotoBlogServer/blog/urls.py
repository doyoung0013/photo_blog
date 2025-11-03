from django.urls import path, include
from rest_framework import routers
from . import views

router = routers.DefaultRouter()
router.register('Post', views.BlogImages)

urlpatterns = [
    path('', views.post_list, name='post_list'),
    path('post/<int:pk>/', views.post_detail, name='post_detail'),
    path('post/new/', views.post_new, name='post_new'),
    path('post/<int:pk>/delete/', views.post_delete, name='post_delete'),
    path('api_root/', include(router.urls)), 
    path('posts/<int:pk>/like/', views.like_post, name='like_post'),
    path('posts/<int:pk>/comment/', views.add_comment, name='add_comment'),
    path('posts/<int:pk>/comments/', views.get_comments, name='get_comments'),
]
