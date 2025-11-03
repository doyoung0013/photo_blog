from django.shortcuts import render, get_object_or_404, redirect
from django.utils import timezone 
from rest_framework import viewsets, status
from rest_framework.decorators import api_view
from rest_framework.response import Response
from .models import Post, Comment
from .serializers import PostSerializer, CommentSerializer
from .forms import PostForm
from django.contrib.auth.models import User

# REST API용 ViewSet
class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.all().order_by('-published_date')
    serializer_class = PostSerializer

    def get_serializer_context(self):
        return {'request': self.request}


# 웹 페이지용 함수형 View들
def post_list(request):
    posts = Post.objects.order_by('-published_date')
    return render(request, 'blog/post_list.html', {'posts': posts})


def post_detail(request, pk):
    post = get_object_or_404(Post, pk=pk)
    comments = post.comments.order_by('-created_at')  # 댓글 최신순 정렬
    return render(request, 'blog/post_detail.html', {'post': post, 'comments': comments})


def post_new(request):
    if request.method == "POST":
        form = PostForm(request.POST, request.FILES)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = User.objects.first()
            post.published_date = timezone.now()
            post.save()
            return redirect('post_detail', pk=post.pk)
    else:
        form = PostForm()
    return render(request, 'blog/post_new.html', {'form': form})


def post_delete(request, pk):
    post = get_object_or_404(Post, pk=pk)
    post.delete()
    return redirect('post_list')


# 좋아요 API (로그인 없이 +1씩 증가)
@api_view(['POST'])
def like_post(request, pk):
    try:
        post = Post.objects.get(pk=pk)
        post.like_count += 1
        post.save()
        return Response({'like_count': post.like_count}, status=status.HTTP_200_OK)
    except Post.DoesNotExist:
        return Response({'error': 'Post not found'}, status=status.HTTP_404_NOT_FOUND)


# 익명 댓글 작성 API
@api_view(['POST'])
def add_comment(request, pk):
    try:
        post = Post.objects.get(pk=pk)
    except Post.DoesNotExist:
        return Response({'error': 'Post not found'}, status=status.HTTP_404_NOT_FOUND)

    serializer = CommentSerializer(data=request.data)
    if serializer.is_valid():
        serializer.save(post=post)
        return Response(serializer.data, status=status.HTTP_201_CREATED)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
